package com.datalinkx.dataserver.health;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXJobException;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.dataserver.bean.domain.JobLogBean;
import com.datalinkx.dataserver.repository.JobLogRepository;
import com.datalinkx.messagehub.transmitter.AlarmProduceTransmitter;
import com.datalinkx.rpc.client.datalinkxjob.DatalinkXJobClient;
import com.datalinkx.rpc.client.flink.FlinkClient;
import com.datalinkx.rpc.client.flink.response.FlinkJobOverview;
import com.datalinkx.dataserver.bean.domain.JobBean;
import com.datalinkx.dataserver.repository.JobRepository;
import com.datalinkx.dataserver.service.StreamJobService;
import com.datalinkx.stream.lock.DistributedLock;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.datalinkx.common.constants.MetaConstants.JobStatus.JOB_STATUS_ERROR;


@Configuration
@Slf4j
public class TaskHealthCheckLoop implements InitializingBean {


    @Autowired
    StreamJobService streamJobService;

    @Autowired
    JobRepository jobRepository;

    @Resource
    DistributedLock distributedLock;

    @Autowired
    FlinkClient flinkClient;

    @Autowired
    DatalinkXJobClient datalinkXJobClient;

    @Autowired
    AlarmProduceTransmitter alarmProduceTransmitter;

    @Autowired
    JobLogRepository jobLogRepository;


    @Override
    public void afterPropertiesSet() {
    }

    @Transactional
    @Scheduled(fixedDelay = 60000) // 每60秒检查
    public void processQueueItems() {
        log.info("start to check stream task status");

        List<JobBean> allTransferJob = jobRepository.findAll();
        List<JobBean> restartJob = new ArrayList<>();
        List<JobBean> statusCheckJob = new ArrayList<>();
        for (JobBean job : allTransferJob) {
            // 流式任务
            if (MetaConstants.JobType.JOB_TYPE_STREAM.equals(job.getType()) && job.getRetryTime() < 5 && job.getStatus()!= MetaConstants.JobStatus.JOB_STATUS_STOP) {
                restartJob.add(job);
            }
            // 需要状态检查的批式任务
            if (!MetaConstants.JobType.JOB_TYPE_STREAM.equals(job.getType()) &&
                    (Arrays.asList(
                            MetaConstants.JobStatus.JOB_STATUS_SYNCING,
                            MetaConstants.JobStatus.JOB_STATUS_QUEUE).contains(job.getStatus()))) {

                statusCheckJob.add(job);
            }
        }

        this.patchJobCheck(statusCheckJob);
        this.streamJobCheck(restartJob);
    }

    // 批式任务状态检查
    @Transactional(rollbackFor = Exception.class, propagation= Propagation.REQUIRES_NEW)
    public void patchJobCheck(List<JobBean> statusCheckJob) {
        for (JobBean jobBean : statusCheckJob) {
            if (MetaConstants.JobStatus.JOB_STATUS_QUEUE == jobBean.getStatus()) {
                // 提交到xxl-job，xxx-job未调度到datalinkx-job
                Timestamp startTime = jobBean.getStartTime();
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());

                long differenceInMillis = Math.abs(currentTime.getTime() - startTime.getTime());

                // 30s内未调度到datalinkx-job，将任务状态置为失败
                if (differenceInMillis > 1 * 1000 * 30) {

                    jobBean.setStatus(MetaConstants.JobStatus.JOB_STATUS_ERROR);
                    jobBean.setErrorMsg("调度组件异常，启动任务失败！！！");
                    jobRepository.save(jobBean);
                }
            }
        }
    }

    // 流式任务检查
    @Transactional(rollbackFor = Exception.class, propagation= Propagation.REQUIRES_NEW)
    public void streamJobCheck(List<JobBean> restartJob) {
        if (ObjectUtils.isEmpty(restartJob)) {
            return;
        }

        try {
            Set<String> runningJobs = this.flinkRunningJobs();
            for (JobBean streamTaskBean : restartJob) {
                String jobId = streamTaskBean.getJobId();

                // 如果datalinkx任务同步中，检查flink任务是否存在
                if (MetaConstants.JobStatus.JOB_STATUS_SYNCING == streamTaskBean.getStatus()) {
                    // 如果flink任务不存在，则重新提交任务
                    if (!runningJobs.contains(jobId)) {

                        this.runStreamTask(runningJobs, jobId);
                        continue;
                    } else {
                        // 如果因为datalinkx挂掉后重启，flink任务正常，datalinkx任务状态正常，判断健康检查线程是否挂掉, 如果挂掉，先停止再重新提交
                        String stringWebResult = datalinkXJobClient.jobHealth(jobId, MetaConstants.JobType.JOB_TYPE_STREAM).getResult();

                        // 排除掉刚提交的任务
                        Timestamp startTime = streamTaskBean.getStartTime();
                        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

                        long differenceInMillis = Math.abs(currentTime.getTime() - startTime.getTime());
                        if (differenceInMillis > 1 * 1000 * 60 && ObjectUtils.isEmpty(stringWebResult)) {
                            this.retryTime(jobId);
                            streamJobService.pause(jobId);
                            continue;
                        }
                    }
                }

                // 如果flink任务在运行，而datalinkx中的任务状态为停止，以datalinkx的状态为准，手动停掉flink任务
                if (runningJobs.contains(jobId) && MetaConstants.JobStatus.JOB_STATUS_STOP == streamTaskBean.getStatus()) {
                    streamJobService.pause(jobId);
                }

                // 如果任务是失败，重新提交
                if (MetaConstants.JobStatus.JOB_STATUS_ERROR == streamTaskBean.getStatus()) {
                    this.retryTime(jobId);
                    this.runStreamTask(runningJobs, jobId);
                }

                if (MetaConstants.JobStatus.JOB_STATUS_CREATE == streamTaskBean.getStatus()) {
                    this.runStreamTask(runningJobs, jobId);
                }
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        }
    }

    /**
     * flink 任务是否运行
     */
    private Set<String> flinkRunningJobs() {
        JsonNode jsonNode = flinkClient.jobOverview();
        return JsonUtils.toList(JsonUtils.toJson(jsonNode.get("jobs")), FlinkJobOverview.class)
                .stream()
                .filter(task -> "RUNNING".equalsIgnoreCase(task.getState()))
                .map(FlinkJobOverview::getName)
                .collect(Collectors.toSet());
    }

    /**
     *  增加重试次数
     * @param jobId
     */
    public void retryTime(String jobId) {
        try {
            JobBean jobBean = jobRepository.findByJobId(jobId).orElseThrow(() -> new DatalinkXJobException("job not found"));
            jobBean.setRetryTime(jobBean.getRetryTime() + 1);
            String errorMsg = "系统异常，流式任务重试";
            if (jobBean.getRetryTime() >= 5) {
                alarmProduceTransmitter.pushAlarmMessage(jobId, JOB_STATUS_ERROR, String.format("实时任务：%s 触发异常，超过最大重试次数！", jobBean.getName()));
                jobBean.setStatus(MetaConstants.JobStatus.JOB_STATUS_ERROR);
                errorMsg = String.format("实时任务：%s 触发异常，超过最大重试次数！", jobBean.getName());
            }
            jobRepository.save(jobBean);
            JobLogBean jobLogBean = JobLogBean.builder()
                    .jobId(jobId)
                    .status(1)
                    .costTime(0)
                    .count("{}")
                    .endTime(new Timestamp(System.currentTimeMillis()))
                    .startTime(new Timestamp(System.currentTimeMillis()))
                    .errorMsg(errorMsg)
                    .build();
            jobLogRepository.save(jobLogBean);
        } catch (DatalinkXJobException e) {
            log.error("Error occurred while retrying job {}", jobId, e);
        }
    }

    /**
     * 提交流式任务
     * @param jobId
     */
    public synchronized void runStreamTask(Set<String> runningJobs, String jobId) {
        String lockId = UUID.randomUUID().toString();
        boolean isLock = distributedLock.lock(jobId, lockId, DistributedLock.STREAM_LOCK_TIME);
        try {
            // 拿到了流式任务的锁就提交任务，任务状态在datalinkx-job提交流程中更改
            if (isLock) {
                // 双重检查
                Optional<JobBean> jobBeanOp = jobRepository.findByJobId(jobId);
                if (!jobBeanOp.isPresent()) {
                    return;
                }

                if (runningJobs.contains(jobId)) {
                    return;
                }

                streamJobService.streamJobExec(jobId, jobId);
                this.retryTime(jobId);
            }
        } catch (Exception e){
            log.error("启动流式任务失败 {}", e.getMessage(), e);
            try {
                // 成功一直持有锁，失败需要释放锁，失败也不需要放入队列，定时任务会从db中扫描出来
                distributedLock.unlock(jobId, lockId);
            } catch (Exception ex) {
                log.error("redis distributedLock error", ex);
            }
        }
    }
}
