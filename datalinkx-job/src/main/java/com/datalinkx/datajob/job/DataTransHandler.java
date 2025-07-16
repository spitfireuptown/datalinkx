package com.datalinkx.datajob.job;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXJobException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.result.WebResult;
import com.datalinkx.common.utils.IdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.datajob.action.AbstractDataTransferAction;
import com.datalinkx.datajob.action.DataTransferAction;
import com.datalinkx.datajob.action.StreamDataTransferAction;
import com.datalinkx.datajob.action.TransformDataTransferAction;
import com.datalinkx.rpc.client.datalinkxserver.DatalinkXServerClient;
import com.datalinkx.rpc.client.datalinkxserver.request.JobStateForm;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Data Trans Job Handler
 */
@RestController
@RequestMapping("/data/transfer")
public class DataTransHandler {
    private static Logger logger = LoggerFactory.getLogger(DataTransHandler.class);

    @Autowired
    private DataTransferAction dataTransferAction;

    @Autowired
    private TransformDataTransferAction transformDataTransferAction;

    @Autowired
    private StreamDataTransferAction streamDataTransferAction;

    @Autowired
    private DatalinkXServerClient dataServerClient;

    public Map<Integer, AbstractDataTransferAction> actionEngine = new ConcurrentHashMap<>();
    @PostConstruct
    public void init() {
        this.actionEngine.put(MetaConstants.JobType.JOB_TYPE_BATCH, dataTransferAction);
        this.actionEngine.put(MetaConstants.JobType.JOB_TYPE_COMPUTE, transformDataTransferAction);
    }

    public DatalinkXJobDetail getJobDetail(String jobId) {
        return dataServerClient.getJobExecInfo(jobId).getResult();
    }

    @SneakyThrows
    @RequestMapping("/stream_exec")
    public void streamJobHandler(String detail) {
        DatalinkXJobDetail datalinkXJobDetail = JsonUtils.toObject(detail, DatalinkXJobDetail.class);
        CompletableFuture.runAsync(() -> {
            try {

                streamDataTransferAction.doAction(datalinkXJobDetail);
            } catch (Exception e) {

                logger.error("异步任务执行失败, jobId: {}", datalinkXJobDetail.getJobId(), e);
            }
        });
    }

    @RequestMapping("/job_health")
    public WebResult<String> jobHealth(String jobId, Integer type) {
        // 如果因为datalinkx挂掉后重启，flink任务正常，datalinkx任务状态正常，判断健康检查线程是否挂掉, 如果挂掉，先停止再重新提交
        Set<Thread> threadsSet = Thread.getAllStackTraces().keySet();
        List<Thread> healthThreads = threadsSet.stream()
                .filter(Thread::isAlive)
                .filter(th -> th.getName().equals(IdUtils.getHealthThreadName(jobId, type)))
                .collect(Collectors.toList());

        if (ObjectUtils.isEmpty(healthThreads)) {
            return WebResult.of("");
        }
        return WebResult.of(healthThreads.get(0).getName());
    }

    /**
     * data trans job
     */
    @XxlJob("datalinkx")
    public void dataTransJobHandler() {
        XxlJobHelper.log("begin dataTransJobHandler. ");
        String jobId = XxlJobHelper.getJobParam();

        // 定时异步调用无法统一trace_id，这里用job_id做trace_id
        MDC.put("trace_id", new Date().getTime() + ":" + jobId);

        long startTime = new Date().getTime();
        DatalinkXJobDetail jobDetail;
        try {
            jobDetail = this.getJobDetail(jobId);
            AbstractDataTransferAction engine = this.actionEngine.get(jobDetail.getType());
            if (ObjectUtils.isEmpty(engine)) {
                throw new DatalinkXJobException("引擎加载失败，检查配置!");
            }
            engine.doAction(jobDetail);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            this.shutdownJob(startTime, jobId, e.getMessage());

            XxlJobHelper.handleFail(e.getMessage());
        }

        XxlJobHelper.log("end dataTransJobHandler. ");
        // default success
        XxlJobHelper.handleSuccess("success");
    }

    private void shutdownJob(long startTime, String jobId, String message) {
        dataServerClient.updateJobStatus(JobStateForm.builder().jobId(jobId)
                .jobStatus(MetaConstants.JobStatus.JOB_STATUS_ERROR).startTime(startTime).endTime(new Date().getTime())
                .errmsg(message)
                .readCount(0L)
                .writeCount(0L)
                .build());
    }
}
