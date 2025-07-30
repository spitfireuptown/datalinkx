package com.datalinkx.dataserver.service.impl;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXServerException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.result.StatusCode;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.dataserver.bean.domain.DsBean;
import com.datalinkx.dataserver.bean.domain.JobBean;
import com.datalinkx.dataserver.bean.vo.JobVo;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.client.JobClientApi;
import com.datalinkx.dataserver.config.properties.CommonProperties;
import com.datalinkx.dataserver.controller.form.JobForm;
import com.datalinkx.dataserver.health.TaskHealthCheckLoop;
import com.datalinkx.dataserver.repository.DsRepository;
import com.datalinkx.dataserver.repository.JobRepository;
import com.datalinkx.dataserver.service.DtsJobService;
import com.datalinkx.dataserver.service.StreamJobService;
import com.datalinkx.rpc.client.datalinkxjob.DatalinkXJobClient;
import com.datalinkx.rpc.client.flink.FlinkClient;
import com.datalinkx.rpc.client.flink.request.FlinkJobStopReq;
import com.datalinkx.rpc.client.flink.response.FlinkJobOverview;
import com.datalinkx.rpc.util.ApplicationContextUtil;
import com.datalinkx.stream.lock.DistributedLock;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.datalinkx.common.constants.MetaConstants.JobStatus.JOB_STATUS_STOP;
import static com.datalinkx.common.utils.IdUtils.genKey;
import static com.datalinkx.common.utils.JsonUtils.toJson;

@Slf4j
@Service
public class StreamJobServiceImpl implements StreamJobService {

    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobServiceImpl jobService;

    @Autowired
    DsRepository dsRepository;

    @Autowired
    DatalinkXJobClient datalinkXJobClient;

    @Autowired
    DtsJobService dtsJobService;

    @Autowired
    JobClientApi jobClientApi;

    @Autowired
    FlinkClient flinkClient;

    @Autowired
    CommonProperties commonProperties;

    @Autowired
    DistributedLock distributedLock;


    @Override
    public String createStreamJob(JobForm.JobCreateForm form) {
        form.setType(MetaConstants.JobType.JOB_TYPE_STREAM);
        jobService.validJobForm(form);
        JobBean jobBean = new JobBean();
        String jobId = genKey("job");
        jobBean.setJobId(jobId);

        jobBean.setType(MetaConstants.JobType.JOB_TYPE_STREAM);
        jobBean.setReaderDsId(form.getFromDsId());
        jobBean.setWriterDsId(form.getToDsId());

        jobBean.setConfig(toJson(form.getFieldMappings()));
        jobBean.setFromTb(form.getFromTbName());
        jobBean.setToTb(form.getToTbName());
        jobBean.setStatus(MetaConstants.JobStatus.JOB_STATUS_CREATE);
        jobBean.setName(form.getJobName());
        jobBean.setSyncMode(JsonUtils.toJson(form.getSyncMode()));

        jobRepository.save(jobBean);
        return jobId;
    }

    @Override
    public String modifyStreamJob(JobForm.JobModifyForm form) {
        jobService.validJobForm(form);
        JobBean jobBean = jobRepository.findByJobId(form.getJobId()).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        jobBean.setReaderDsId(form.getFromDsId());
        jobBean.setWriterDsId(form.getToDsId());
        jobBean.setConfig(toJson(form.getFieldMappings()));
        jobBean.setFromTb(form.getFromTbName());
        jobBean.setToTb(form.getToTbName());
        jobBean.setName(form.getJobName());
        jobBean.setSyncMode(JsonUtils.toJson(form.getSyncMode()));
        jobRepository.save(jobBean);
        return form.getJobId();
    }

    @Override
    public void startStreamJob(String jobId) {
        JobBean jobBean = jobRepository.findByJobId(jobId).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        if (MetaConstants.JobStatus.JOB_STATUS_SYNCING == jobBean.getStatus()) {
            throw new DatalinkXServerException(StatusCode.JOB_IS_RUNNING, "任务运行中");
        }
        if (MetaConstants.JobStatus.JOB_STATUS_QUEUE == jobBean.getStatus()) {
            throw new DatalinkXServerException(StatusCode.JOB_IS_RUNNING, "任务排队中");
        }
        if (MetaConstants.JobStatus.JOB_STATUS_STOPPING == jobBean.getStatus()) {
            throw new DatalinkXServerException(StatusCode.JOB_IS_RUNNING, "任务正在停止中");
        }

        jobBean.setRetryTime(0);
        jobBean.setStatus(MetaConstants.JobStatus.JOB_STATUS_SYNCING);
        jobRepository.save(jobBean);

        TaskHealthCheckLoop taskHealthCheckLoop = ApplicationContextUtil.getBean(TaskHealthCheckLoop.class);
        taskHealthCheckLoop.processQueueItems();
    }

    @Override
    public PageVo<List<JobVo.JobStreamPageVo>> streamPage(JobForm.JobPageForm form) {
        PageRequest pageRequest = PageRequest.of(form.getPageNo() - 1, form.getPageSize());
        Page<JobBean> jobBeans = jobRepository.pageQuery(pageRequest, form.getType());


        List<String> dsId = new ArrayList<>();
        jobBeans.getContent().forEach(job -> {
            dsId.add(job.getWriterDsId());
            dsId.add(job.getReaderDsId());
        });


        Map<String, String> dsNameMap = dsRepository.findAllByDsIdIn(dsId)
                .stream()
                .collect(Collectors.toMap(DsBean::getDsId, DsBean::getName));

        List<JobVo.JobStreamPageVo> pageVoList = jobBeans.getContent().stream().map(jobBean -> JobVo.JobStreamPageVo
                .builder()
                .jobId(jobBean.getJobId())
                .jobName(jobBean.getName())
                .fromTbName(dsNameMap.get(jobBean.getReaderDsId()) + "." + jobBean.getFromTb())
                .toTbName(dsNameMap.get(jobBean.getWriterDsId()) + "."  + jobBean.getToTb())
                .startTime(jobBean.getStartTime())
                .retryTime(jobBean.getRetryTime())
                .status(jobBean.getStatus())
                .build()).collect(Collectors.toList());

        PageVo<List<JobVo.JobStreamPageVo>> result = new PageVo<>();
        result.setPageNo(form.getPageNo());
        result.setPageSize(form.getPageSize());
        result.setData(pageVoList);
        result.setTotalPage(jobBeans.getTotalPages());
        result.setTotal(jobBeans.getTotalElements());
        return result;
    }

    @Async
    @Override
    public void streamJobExec(String jobId, String lockId) {
        DatalinkXJobDetail jobExecInfo = dtsJobService.getStreamJobExecInfo(jobId);
        jobExecInfo.setLockId(lockId);
        datalinkXJobClient.dataTransExec(JsonUtils.toJson(jobExecInfo));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void stop(String jobId) {
        JobBean jobBean = jobRepository.findByJobId(jobId).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        if (MetaConstants.JobStatus.JOB_STATUS_STOPPING == jobBean.getStatus()) {
            return;
        }

        // 记录checkpoint
        this.stopFlinkTask(jobBean);

        jobBean.setStatus(MetaConstants.JobStatus.JOB_STATUS_STOPPING);
        jobRepository.save(jobBean);
    }

    /**
     * 停止flink任务
     */
    private void stopFlinkTask(JobBean jobBean) {
        // 记录checkpoint
        if (!ObjectUtils.isEmpty(jobBean.getTaskId())) {
            FlinkJobStopReq flinkJobStopReq = new FlinkJobStopReq();
            flinkJobStopReq.setDrain(true);
            String checkpoint = String.format("%s/%s", commonProperties.getCheckpointPath(), jobBean.getJobId());

            flinkJobStopReq.setTargetDirectory(checkpoint);
            flinkClient.jobStop(jobBean.getTaskId(), flinkJobStopReq);
        }
    }

    @Override
    public void pause(String jobId) {
        JobBean jobBean = jobRepository.findByJobId(jobId).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        if (!ObjectUtils.isEmpty(jobBean.getTaskId())) {
            this.stopFlinkTask(jobBean);
            // 记录checkpoint
            jobRepository.save(jobBean);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String jobId) {
        this.stop(jobId);
        jobService.del(jobId, true);
    }
}
