package com.datalinkx.datajob.action;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXJobException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ObjectUtils;

import com.datalinkx.compute.connector.jdbc.TransformNode;
import com.datalinkx.compute.transform.ITransformDriver;
import com.datalinkx.compute.transform.ITransformFactory;
import com.datalinkx.messagehub.transmitter.AlarmProduceTransmitter;
import com.datalinkx.driver.dsdriver.DsDriverFactory;
import com.datalinkx.driver.dsdriver.IDsReader;
import com.datalinkx.driver.dsdriver.IDsWriter;
import com.datalinkx.driver.dsdriver.base.meta.SeatunnelActionMeta;
import com.datalinkx.rpc.client.datalinkxserver.DatalinkXServerClient;
import com.datalinkx.rpc.client.datalinkxserver.request.JobStateForm;
import com.datalinkx.rpc.client.datalinkxserver.request.JobSyncModeForm;
import com.datalinkx.rpc.client.seatunnel.SeaTunnelClient;
import com.datalinkx.rpc.client.seatunnel.request.ComputeJobGraph;
import com.datalinkx.rpc.client.seatunnel.response.JobCommitResp;
import com.datalinkx.rpc.client.seatunnel.response.JobOverviewResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.datalinkx.common.constants.MetaConstants.JobStatus.JOB_STATUS_SUCCESS;


@Slf4j
@Component
public class TransformDataTransferAction extends AbstractDataTransferAction<DatalinkXJobDetail, SeatunnelActionMeta> {

    @Autowired
    SeaTunnelClient seaTunnelClient;
    @Autowired
    DatalinkXServerClient datalinkXServerClient;
    @Autowired
    AlarmProduceTransmitter alarmProduceTransmitter;

    @Override
    protected void begin(DatalinkXJobDetail info) {
        datalinkXServerClient.updateJobStatus(JobStateForm.builder().jobId(info.getJobId())
                .jobStatus(MetaConstants.JobStatus.JOB_STATUS_SYNCING).startTime(new Date().getTime())
                .build());
    }

    @Override
    protected void end(SeatunnelActionMeta unit, int status, String errmsg) {
        log.info(String.format("transform job jobid: %s, end to transfer", unit.getJobId()));
        datalinkXServerClient.updateJobStatus(JobStateForm.builder().jobId(unit.getJobId())
                .jobStatus(status).endTime(new Date().getTime())
                .errmsg(errmsg)
                .build());

        // 父任务执行成功后级联触发子任务
        if (JOB_STATUS_SUCCESS == status) {
            datalinkXServerClient.cascadeJob(unit.getJobId());
        }
    }

    @Override
    protected void beforeExec(SeatunnelActionMeta unit) throws Exception {
        IDsWriter writeDsDriver = DsDriverFactory.getDsWriter(unit.getWriter().getConnectId());
        // 是否覆盖数据
        if (unit.getCover() == 1) {
            writeDsDriver.truncateData(unit.getWriter());
        }
    }

    @Override
    protected void execute(SeatunnelActionMeta unit) throws Exception {
        ComputeJobGraph computeJobGraph = new ComputeJobGraph();
        computeJobGraph.setJobId(unit.getJobId());
        computeJobGraph.setEnv(new HashMap<String, Object>() {{
            put("job.mode", unit.getJobMode());
        }});
        computeJobGraph.setSource(Collections.singletonList(unit.getSourceInfo()));
        computeJobGraph.setTransform(unit.getTransformInfo().stream()
                .map(node -> (Object) node) // 使用 map 将 TransformNode 转换为 Object
                .collect(Collectors.toList()));
        computeJobGraph.setSink(Collections.singletonList(unit.getSinkInfo()));
        log.info("job_graph ==> {}", computeJobGraph);
        JobCommitResp jobCommitResp = seaTunnelClient.jobSubmit(computeJobGraph);
        String taskId = jobCommitResp.getJobId();
        unit.setTaskId(taskId);
        // 更新task
        datalinkXServerClient.updateJobTaskRel(unit.getJobId(), taskId);
    }

    @Override
    protected boolean checkResult(SeatunnelActionMeta unit) throws DatalinkXJobException {
        String taskId = unit.getTaskId();
        if (ObjectUtils.isEmpty(taskId)) {
            throw new DatalinkXJobException("task id is empty.");
        }

        JobOverviewResp jobOverviewResp = seaTunnelClient.jobOverview(taskId);
        if (MetaConstants.JobStatus.SEATUNNEL_JOB_FINISH.equalsIgnoreCase(jobOverviewResp.getJobStatus())) {
            return true;
        }

        if (MetaConstants.JobStatus.SEATUNNEL_JOB_FAILED.equalsIgnoreCase(jobOverviewResp.getJobStatus())) {
            String errorMsg = "请检查Seatunnel日志";
            if (!ObjectUtils.isEmpty(jobOverviewResp.getErrorMsg())) {
                errorMsg = jobOverviewResp.getErrorMsg();
            }
            log.error(errorMsg);
            throw new DatalinkXJobException(errorMsg);
        }

        return false;
    }

    @Override
    protected void afterExec(SeatunnelActionMeta unit, boolean success) {
        // 记录增量记录
        if (success) {
            datalinkXServerClient.updateSyncMode(
                    JobSyncModeForm.builder()
                            .jobId(unit.getJobId())
                            .increateValue(
                                    unit.getReader().getMaxValue()
                            ).build());
        }
    }

    @Override
    protected SeatunnelActionMeta convertExecUnit(DatalinkXJobDetail info) throws Exception {
        IDsReader dsReader = DsDriverFactory.getDsReader(info.getSyncUnit().getReader().getConnectId());
        IDsWriter dsWriter = DsDriverFactory.getDsWriter(info.getSyncUnit().getWriter().getConnectId());

        Map<String, Object> commonSettings = info.getSyncUnit().getCommonSettings();
        List<TransformNode> transformNodes = new ArrayList<>();
        String lastTransformNodeName = "";

        for (DatalinkXJobDetail.Compute.Transform transform : info.getSyncUnit().getCompute().getTransforms()) {
            ITransformDriver computeDriver = ITransformFactory.getComputeDriver(transform.getType());
            TransformNode transformNode = computeDriver.transferInfo(commonSettings, transform.getMeta());
            lastTransformNodeName = transformNode.getResultTableName();
            transformNodes.add(transformNode);
        }

        SeatunnelActionMeta seatunnelActionMeta = SeatunnelActionMeta.builder()
                .writer(info.getSyncUnit().getWriter())
                .reader(info.getSyncUnit().getReader())
                .transformInfo(transformNodes)
                .jobMode("batch")
                .jobId(info.getJobId())
                .cover(info.getCover())
                .parallelism(1)
                .build();

        TransformNode sinkInfo = dsWriter.getSinkInfo(info.getSyncUnit().getWriter());
        sinkInfo.setSourceTableName(lastTransformNodeName);


        seatunnelActionMeta.setSourceInfo(dsReader.getSourceInfo(info.getSyncUnit().getReader()));
        seatunnelActionMeta.setSinkInfo(sinkInfo);

        return seatunnelActionMeta;
    }

    @Override
    protected void destroyed(SeatunnelActionMeta unit, int status, String errmsg) {
        alarmProduceTransmitter.pushAlarmMessage(unit.getJobId(), status, errmsg);
    }
}
