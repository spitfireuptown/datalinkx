
package com.datalinkx.datajob.action;

import com.datalinkx.common.constants.MessageHubConstants;
import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXJobException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.datajob.job.ExecutorJobHandler;
import com.datalinkx.messagehub.transmitter.AlarmProduceTransmitter;
import com.datalinkx.driver.dsdriver.DsDriverFactory;
import com.datalinkx.driver.dsdriver.IDsWriter;
import com.datalinkx.driver.dsdriver.base.meta.FlinkActionMeta;
import com.datalinkx.messagehub.bean.form.ProducerAdapterForm;
import com.datalinkx.messagehub.service.MessageHubService;
import com.datalinkx.rpc.client.datalinkxserver.DatalinkXServerClient;
import com.datalinkx.rpc.client.datalinkxserver.request.JobStateForm;
import com.datalinkx.rpc.client.datalinkxserver.request.JobSyncModeForm;
import com.datalinkx.rpc.client.flink.FlinkClient;
import com.datalinkx.rpc.client.flink.response.FlinkJobAccumulators;
import com.datalinkx.rpc.client.flink.response.FlinkJobStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.datalinkx.common.constants.MetaConstants.JobStatus.JOB_STATUS_SUCCESS;

@Slf4j
@Component
public class DataTransferAction extends AbstractDataTransferAction<DatalinkXJobDetail, FlinkActionMeta> {

    @Autowired
    private ExecutorJobHandler executorJobHandler;

    @Autowired
    private FlinkClient flinkClient;

    @Autowired
    private DatalinkXServerClient datalinkXServerClient;


    @Resource(name = "messageHubServiceImpl")
    MessageHubService messageHubService;

    @Autowired
    AlarmProduceTransmitter alarmProduceTransmitter;


    @Override
    protected void begin(DatalinkXJobDetail info) {
        // 更新同步任务的状态
        log.info(String.format("jobid: %s, start to transfer", info.getJobId()));
        datalinkXServerClient.updateJobStatus(
                JobStateForm.builder()
                        .jobId(info.getJobId())
                        .jobStatus(MetaConstants.JobStatus.JOB_STATUS_SYNCING)
                        .startTime(new Date().getTime())
                        .endTime(null)
                        .build()
        );
    }

    @Override
    protected void end(FlinkActionMeta unit, int status, String errmsg) {
        log.info(String.format("jobid: %s, end to transfer", unit.getJobId()));

        datalinkXServerClient.updateJobStatus(JobStateForm.builder().jobId(unit.getJobId())
                .jobStatus(status)
                .endTime(new Date().getTime())
                .readCount(unit.getReadRecords())
                .writeCount(unit.getWriteRecords())
                .errmsg(errmsg)
                .build());
        // 父任务执行成功后级联触发子任务
        if (JOB_STATUS_SUCCESS == status) {
            datalinkXServerClient.cascadeJob(unit.getJobId());
        }
    }

    @Override
    protected void beforeExec(FlinkActionMeta unit) throws Exception {
        log.info(String.format("jobid: %s, begin from %s to %s", unit.getJobId(), unit.getReader().getTableName(), unit.getWriter().getTableName()));
        // 是否覆盖数据
        if (unit.getCover() == 1) {
            IDsWriter writeDsDriver = DsDriverFactory.getDsWriter(unit.getWriter().getConnectId());
            writeDsDriver.truncateData(unit.getWriter());
        }
    }


    @Override
    protected void execute(FlinkActionMeta unit) throws Exception {
        log.info(String.format("jobid: %s, exec from %s#%s to %s#%s",
                unit.getJobId(),
                unit.getReader().getSchema(),
                unit.getReader().getTableName(),
                unit.getWriter().getSchema(),
                unit.getWriter().getTableName())
        );
        String taskId = unit.getTaskId();
        try {
            if (!ObjectUtils.isEmpty(taskId)) {
                return;
            }

            taskId = executorJobHandler.execute(
                    unit.getJobId(),
                    JsonUtils.toJson(unit.getReader().getReaderGraph()),
                    JsonUtils.toJson(unit.getWriter().getWriterGraph()),
                    new HashMap<>()
            );
            unit.setTaskId(taskId) ;
            // 更新task
            datalinkXServerClient.updateJobTaskRel(unit.getJobId(), taskId);
        } catch (DatalinkXJobException e) {
            log.error("data transfer failed", e);
            throw e;
        }
    }

    @Override
    protected boolean checkResult(FlinkActionMeta unitParam) throws DatalinkXJobException {
        String taskId = unitParam.getTaskId();
        if (ObjectUtils.isEmpty(taskId)) {
            throw new DatalinkXJobException("task id is empty.");
        }


        FlinkJobStatus flinkJobStatus = JsonUtils.toObject(JsonUtils.toJson(flinkClient.jobStatus(taskId)), FlinkJobStatus.class);
        String state = flinkJobStatus.getState();

        if ("finished".equalsIgnoreCase(state)) {
            computeRecords(unitParam);
            return true;
        }

        if ("failed".equalsIgnoreCase(state)) {
            String errorMsg = "datalinkx task failed.";

            JsonNode jsonNode = flinkClient.jobExceptions(taskId);
            if (jsonNode.has("all-exceptions")) {
                Iterator<JsonNode> exceptions = jsonNode.get("all-exceptions").elements();
                if (exceptions.hasNext()) {
                    errorMsg = exceptions.next().get("exception").asText();
                }
            }
            log.error(errorMsg);
            throw new DatalinkXJobException(errorMsg);
        }

        if ("canceled".equalsIgnoreCase(state)) {
            log.error("datalinkx task canceled.");
            throw new DatalinkXJobException("datalinkx task canceled.");
        }

        computeRecords(unitParam);
        return false;
    }

    private void computeRecords(FlinkActionMeta unitParam) {
        AtomicInteger readRecords = new AtomicInteger(0);
        AtomicInteger writeRecords = new AtomicInteger(0);
        AtomicInteger errorRecords = new AtomicInteger(0);
        AtomicLong bytes = new AtomicLong(0);

        FlinkJobAccumulators flinkJobAccumulators = JsonUtils.toObject(
                JsonUtils.toJson(
                        flinkClient.jobAccumulators(unitParam.getTaskId())
                ),
                FlinkJobAccumulators.class
        );
        for (FlinkJobAccumulators.UserTaskAccumulator userTaskAccumulator : Optional.ofNullable(flinkJobAccumulators.getUserTaskAccumulators()).orElse(new ArrayList<>())) {
            if ("numWrite".equals(userTaskAccumulator.getName())) {
                writeRecords = new AtomicInteger(Integer.parseInt(userTaskAccumulator.getValue()));
            }
            else if ("numRead".equals(userTaskAccumulator.getName())) {
                readRecords = new AtomicInteger(Integer.parseInt(userTaskAccumulator.getValue()));
            }
            else if ("nErrors".equals(userTaskAccumulator.getName())) {
                errorRecords = new AtomicInteger(Integer.parseInt(userTaskAccumulator.getValue()));
            }
        }

        unitParam.setReadRecords(readRecords.get());
        unitParam.setWriteRecords(writeRecords.get() - errorRecords.get());

        // 实时推送流转进度
        ProducerAdapterForm producerAdapterForm = new ProducerAdapterForm();
        producerAdapterForm.setType(MessageHubConstants.REDIS_STREAM_TYPE);
        producerAdapterForm.setTopic(MessageHubConstants.JOB_PROGRESS_TOPIC);
        producerAdapterForm.setGroup(MessageHubConstants.GLOBAL_COMMON_GROUP);
        Map<String, Object> jobProgress = new HashMap<String, Object>() {{
            put("job_id", unitParam.getJobId());
            put("status", 1);
            put("read_records", unitParam.getReadRecords());
            put("write_records", unitParam.getWriteRecords());
        }};
        producerAdapterForm.setMessage(JsonUtils.toJson(jobProgress));
        messageHubService.produce(producerAdapterForm);
    }

    @Override
    protected void afterExec(FlinkActionMeta unit, boolean success) {
        // 记录增量记录
        datalinkXServerClient.updateSyncMode(
                JobSyncModeForm.builder()
                        .jobId(unit.getJobId())
                        .increateValue(
                                unit.getReader().getMaxValue()
                        ).build());
    }

    @Override
    protected FlinkActionMeta convertExecUnit(DatalinkXJobDetail jobDetail) {
        return FlinkActionMeta.builder()
                    .reader(jobDetail.getSyncUnit().getReader())
                    .writer(jobDetail.getSyncUnit().getWriter())
                    .jobId(jobDetail.getJobId())
                    .cover(jobDetail.getCover())
                    .build();
    }

    @Override
    protected void destroyed(FlinkActionMeta unit, int status, String errmsg) {
        alarmProduceTransmitter.pushAlarmMessage(unit.getJobId(), status, errmsg);
    }
}
