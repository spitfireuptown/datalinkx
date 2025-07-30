package com.datalinkx.datajob.action;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXJobException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.datajob.job.ExecutorStreamJobHandler;

import com.datalinkx.driver.dsdriver.DsDriverFactory;
import com.datalinkx.driver.dsdriver.base.meta.StreamFlinkActionMeta;
import com.datalinkx.rpc.client.datalinkxserver.DatalinkXServerClient;
import com.datalinkx.rpc.client.datalinkxserver.request.JobStateForm;
import com.datalinkx.rpc.client.flink.FlinkClient;
import com.datalinkx.rpc.client.flink.request.FlinkSavepointReq;
import com.datalinkx.rpc.client.flink.response.FlinkJobStatus;
import com.datalinkx.stream.lock.DistributedLock;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import static com.datalinkx.common.constants.MessageHubConstants.GLOBAL_COMMON_GROUP;

/**
 * @author: uptown
 * @date: 2024/4/27 14:23
 */
@Slf4j
@Component
public class StreamDataTransferAction extends AbstractDataTransferAction<DatalinkXJobDetail, StreamFlinkActionMeta> {
    @Autowired
    FlinkClient flinkClient;

    @Autowired
    DatalinkXServerClient datalinkXServerClient;

    @Autowired
    ExecutorStreamJobHandler streamExecutorJobHandler;

    @Autowired
    DistributedLock distributedLock;

    @Override
    protected void begin(DatalinkXJobDetail info) {
        // 修改任务状态
        datalinkXServerClient.updateJobStatus(JobStateForm.builder().jobId(info.getJobId())
                .jobStatus(MetaConstants.JobStatus.JOB_STATUS_SYNCING).startTime(new Date().getTime())
                .build());
    }

    @Override
    protected void end(StreamFlinkActionMeta unit, int status, String errmsg) {
        log.info(String.format("stream job jobid: %s, end to transfer", unit.getJobId()));
        // 修改任务状态，存储checkpoint
        datalinkXServerClient.updateJobStatus(JobStateForm.builder().jobId(unit.getJobId())
                .jobStatus(status).endTime(new Date().getTime())
                .checkpoint(unit.getCheckpointPath())
                .errmsg(errmsg)
                .build());
    }

    @Override
    protected void beforeExec(StreamFlinkActionMeta unit) throws Exception {
    }

    @Override
    protected void execute(StreamFlinkActionMeta unit) throws Exception {
        synchronized (this) {
            Map<String, Object> commonSettings = unit.getCommonSettings();
            commonSettings.put("savePointPath", unit.getCheckpointPath());
            String taskId = streamExecutorJobHandler.execute(unit.getJobId(), unit.getReaderDsInfo(), unit.getWriterDsInfo(), commonSettings);
            unit.setTaskId(taskId);
            // 更新task
            datalinkXServerClient.updateJobTaskRel(unit.getJobId(), taskId);
        }
    }

    @Override
    protected boolean checkResult(StreamFlinkActionMeta unit) {
        String taskId = unit.getTaskId();
        FlinkJobStatus flinkJobStatus = JsonUtils.toObject(JsonUtils.toJson(flinkClient.jobStatus(taskId)), FlinkJobStatus.class);
        String state = flinkJobStatus.getState();
        if ("failed".equalsIgnoreCase(state)) {
            String errorMsg = "stream data-transfer task failed.";

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
            log.error("data-transfer task canceled.");
            throw new DatalinkXJobException("data-transfer task canceled.");
        }

        if ("finished".equalsIgnoreCase(state)) {
            return true;
        }
        // 看门狗，续约分布式锁，防止其他节点重复提交任务
        distributedLock.renewLock(unit.getJobId(), unit.getLockId(), DistributedLock.STREAM_LOCK_TIME);

        // 定时备份checkpoint
        Boolean checkpointEnable = (Boolean) unit.getCommonSettings().get(MetaConstants.CommonConstant.KEY_CHECKPOINT_ENABLE);
        String checkpointPath = ObjectUtils.isEmpty(unit.getCheckpointPath())
                ? (String) unit.getCommonSettings().get(MetaConstants.CommonConstant.KEY_CHECKPOINT_INIT_ADDRESS)
                : unit.getCheckpointPath();

        if (!ObjectUtils.isEmpty(checkpointEnable) && checkpointEnable) {
            flinkClient.jobSavePoints(
                    unit.getTaskId(),
                    FlinkSavepointReq
                            .builder()
                            .targetDirectory(checkpointPath)
                            .cancelJob(false)
                            .build()
            );
        }

        // 流式任务不用检测太频繁，歇会
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    protected void afterExec(StreamFlinkActionMeta unit, boolean success) {
        // 任务未提交成功
        if (ObjectUtils.isEmpty(unit.getTaskId())) {
            return;
        }
        // 记录checkpoint
        JsonNode checkpointResult = flinkClient.jobCheckpoint(unit.getTaskId());
        if (!ObjectUtils.isEmpty(checkpointResult)) {
            JsonNode latestResult = checkpointResult.get("latest");
            if (!ObjectUtils.isEmpty(latestResult)) {
                JsonNode savepoint = latestResult.get("savepoint");
                if (!ObjectUtils.isEmpty(savepoint) && !ObjectUtils.isEmpty(savepoint.get("external_path"))) {
                    String checkpointPath = savepoint.get("external_path").toString();
                    unit.setCheckpointPath(checkpointPath);
                }
            }
        }
    }

    @SneakyThrows
    @Override
    protected StreamFlinkActionMeta convertExecUnit(DatalinkXJobDetail info) {
        Object readerDsInfo = DsDriverFactory.getStreamDriver(info.getSyncUnit().getReader().getConnectId()).getReaderInfo(info.getSyncUnit().getReader());

        // 实时任务的writer不一定是流式数据源
        // 可能是binlog -> oracle，binlog -> es
        // 也可能是binlog -> binlog
        Object writerDsInfo;
        if (MetaConstants.DsType.STREAM_DB_LIST.contains(info.getSyncUnit().getWriter().getType())) {
            writerDsInfo = DsDriverFactory.getStreamDriver(info.getSyncUnit().getWriter().getConnectId()).getWriterInfo(info.getSyncUnit().getWriter());
        } else {

            DatalinkXJobDetail.Writer writer = DatalinkXJobDetail
                    .Writer
                    .builder()
                    .type(info.getSyncUnit().getWriter().getType())
                    .connectId(info.getSyncUnit().getWriter().getConnectId())
                    .tableName(info.getSyncUnit().getWriter().getTableName())
                    .schema(info.getSyncUnit().getWriter().getSchema())
                    .columns(info.getSyncUnit().getWriter().getColumns())
                    .batchSize(info.getSyncUnit().getWriter().getBatchSize())
                    // 如果是binlog2jdbc的writemode应为updte
                    .writeMode("update")
                    .updateKey(info.getSyncUnit().getWriter().getUpdateKey())
                    .build();
            writerDsInfo = DsDriverFactory.getDsWriter(info.getSyncUnit().getWriter().getConnectId()).getWriterInfo(writer);
        }

        return StreamFlinkActionMeta.builder()
                .writerDsInfo(JsonUtils.toJson(writerDsInfo))
                .readerDsInfo(JsonUtils.toJson(readerDsInfo))
                .checkpointPath(info.getSyncUnit().getCheckpointPath())
                .commonSettings(info.getSyncUnit().getCommonSettings())
                .jobId(info.getJobId())
                .lockId(info.getLockId())
                .build();
    }

    @Override
    protected void destroyed(StreamFlinkActionMeta unit, int status, String errmsg) {
        distributedLock.unlock(unit.getJobId(), unit.getLockId());
    }
}
