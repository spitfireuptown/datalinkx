package com.datalinkx.dataserver.service.impl;

import com.datalinkx.common.constants.MessageHubConstants;
import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXServerException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.result.StatusCode;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.compute.transform.ITransformDriver;
import com.datalinkx.compute.transform.ITransformFactory;
import com.datalinkx.dataserver.bean.domain.DsBean;
import com.datalinkx.dataserver.bean.domain.JobBean;
import com.datalinkx.dataserver.bean.domain.JobLogBean;
import com.datalinkx.dataserver.bean.domain.JobRelationBean;
import com.datalinkx.dataserver.bean.dto.JobDto;
import com.datalinkx.dataserver.client.JobClientApi;
import com.datalinkx.dataserver.config.properties.CommonProperties;
import com.datalinkx.dataserver.controller.form.JobForm;
import com.datalinkx.dataserver.controller.form.JobStateForm;
import com.datalinkx.dataserver.repository.DsRepository;
import com.datalinkx.dataserver.repository.JobLogRepository;
import com.datalinkx.dataserver.repository.JobRelationRepository;
import com.datalinkx.dataserver.repository.JobRepository;
import com.datalinkx.dataserver.service.DtsJobService;
import com.datalinkx.driver.dsdriver.DsDriverFactory;
import com.datalinkx.driver.dsdriver.IDsReader;
import com.datalinkx.driver.dsdriver.IDsWriter;
import com.datalinkx.driver.dsdriver.base.meta.DbTableField;
import com.datalinkx.messagehub.bean.form.ProducerAdapterForm;
import com.datalinkx.messagehub.service.MessageHubService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.datalinkx.common.constants.MetaConstants.JobStatus.JOB_STATUS_QUEUE;
import static com.datalinkx.common.constants.MetaConstants.JobStatus.JOB_STATUS_SYNCING;

/**
 * @author: uptown
 * @date: 2024/4/25 22:36
 */
@Service
public class DtsJobServiceImpl implements DtsJobService {

    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobRelationRepository jobRelationRepository;


    @Autowired
    JobLogRepository jobLogRepository;


    @Autowired
    DsServiceImpl dsServiceImpl;

    @Autowired
    DsRepository dsRepository;

    @Resource(name = "messageHubServiceImpl")
    MessageHubService messageHubService;

    @Autowired
    JobClientApi jobClientApi;

    @Autowired
    CommonProperties commonProperties;

    @Override
    public DatalinkXJobDetail getJobExecInfo(String jobId) {
        JobBean jobBean = jobRepository.findByJobId(jobId).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        List<JobForm.FieldMappingForm> fieldMappingForms = JsonUtils.toList(jobBean.getConfig(), JobForm.FieldMappingForm.class);

        DatalinkXJobDetail.SyncUnit syncUnit = DatalinkXJobDetail.SyncUnit
                .builder()
                .reader(this.getReader(jobBean, fieldMappingForms))
                .writer(this.getWriter(jobBean, fieldMappingForms))
                .build();

        // 解析计算任务图
        this.analysisComputeGraph(syncUnit, jobBean.getGraph());
        return DatalinkXJobDetail.builder().jobId(jobId).type(jobBean.getType()).cover(jobBean.getCover()).syncUnit(syncUnit).build();
    }

    @Override
    public DatalinkXJobDetail getStreamJobExecInfo(String jobId) {
        JobBean jobBean = jobRepository.findByJobId(jobId).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        List<String> fromColumns = new ArrayList<>();
        List<String> toColumns = new ArrayList<>();

        JsonUtils.toList(jobBean.getConfig(), JobForm.FieldMappingForm.class).stream()
        .filter(x -> StringUtils.isNotEmpty(x.getSourceField()) && StringUtils.isNotEmpty(x.getTargetField()))
        .forEach(x -> {
            fromColumns.add(x.getSourceField());
            toColumns.add(x.getTargetField());
        });

        JobForm.SyncModeForm syncModeForm = JsonUtils.toObject(jobBean.getSyncMode(), JobForm.SyncModeForm.class);
        Map<String, Object> commonSettings = new HashMap<>();
        commonSettings.put(MetaConstants.CommonConstant.KEY_KAFKA_READ_INDEX, commonProperties.getKafkaReadMode());
        commonSettings.put(MetaConstants.CommonConstant.KEY_CHECKPOINT_INTERVAL, commonProperties.getCheckpointInterval());
        commonSettings.put(MetaConstants.CommonConstant.KEY_RESTORE_COLUMN_INDEX, syncModeForm.getRestoreColumnIndex());
        commonSettings.put(MetaConstants.CommonConstant.KEY_CHECKPOINT_ENABLE, syncModeForm.getCheckpoint());
        commonSettings.put(MetaConstants.CommonConstant.KEY_CHECKPOINT_INIT_ADDRESS, String.format("%s/%s", commonProperties.getCheckpointPath(), jobBean.getJobId()));

        List<String> dsIds = new ArrayList<>(Arrays.asList(jobBean.getReaderDsId(), jobBean.getWriterDsId()));
        Map<String, DsBean> dsId2Object = dsRepository.findAllByDsIdIn(dsIds)
                .stream()
                .collect(Collectors.toMap(DsBean::getDsId, v -> v));

        DatalinkXJobDetail.Reader reader = DatalinkXJobDetail.Reader.builder()
                .tableName(jobBean.getFromTb())
                .columns(fromColumns)
                .connectId(dsServiceImpl.getConnectId(dsId2Object.get(jobBean.getReaderDsId())))
                .type(dsId2Object.get(jobBean.getReaderDsId()).getType())
                .build();

        DatalinkXJobDetail.Writer writer = DatalinkXJobDetail.Writer.builder()
                .tableName(jobBean.getToTb())
                .columns(toColumns)
                .updateKey(syncModeForm.getUpdateKey())
                .connectId(dsServiceImpl.getConnectId(dsId2Object.get(jobBean.getWriterDsId())))
                .batchSize(commonProperties.getStreamBatchSize())
                .type(dsId2Object.get(jobBean.getWriterDsId()).getType())
                .build();

        DatalinkXJobDetail.SyncUnit syncUnit = DatalinkXJobDetail.SyncUnit
                .builder()
                .reader(reader)
                .writer(writer)
                .commonSettings(commonSettings)
                .checkpointPath(jobBean.getCheckpoint())
                .build();

        // 如果开启断点续传
        if (syncModeForm.getCheckpoint() && !ObjectUtils.isEmpty(syncUnit.getCheckpointPath())) {
            String checkpoint = syncUnit.getCheckpointPath().replace("file:/", "");
            File directory = new File(checkpoint);
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (!ObjectUtils.isEmpty(files)) {
                    // 按最后修改时间降序排序（最近修改的文件在前）
                    Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    syncUnit.setCheckpointPath(files[0].getPath());
                }
            } else {
                syncUnit.setCheckpointPath("");
            }
        }
        return DatalinkXJobDetail.builder().jobId(jobId).type(MetaConstants.JobType.JOB_TYPE_STREAM).syncUnit(syncUnit).build();
    }


    @SneakyThrows
    private DatalinkXJobDetail.Reader getReader(JobBean jobBean,
                                                List<JobForm.FieldMappingForm> jobConf) {

        DsBean fromDs = dsRepository
                .findByDsId(jobBean.getReaderDsId())
                .orElseThrow(
                        () -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS, "from ds not exist")
                );

        JobDto.JobGraphDto jobGraphInfo = dsServiceImpl.getJobGraphInfo(fromDs);


        // 流转任务来源表字段列表
        List<String> fromCols = jobConf.stream()
                .filter(x -> StringUtils.isNotEmpty(x.getSourceField()) && StringUtils.isNotEmpty(x.getTargetField()))
                .map(JobForm.FieldMappingForm::getSourceField)
                .collect(Collectors.toList());

        JobForm.SyncModeForm syncModeForm = JsonUtils.toObject(jobBean.getSyncMode(), JobForm.SyncModeForm.class);

        DatalinkXJobDetail.TransferSetting transferSetting = DatalinkXJobDetail.TransferSetting
                .builder()
                .type(syncModeForm.getMode())
                .queryTimeOut(commonProperties.getQueryTimeOut())
                .fetchSize(commonProperties.getFetchSize())
                .build();

        // 获取对应增量条件
        IDsReader dsReader = DsDriverFactory.getDsReader(dsServiceImpl.getConnectId(fromDs));
        Map<String, String> typeMappings = dsReader.getFields(jobGraphInfo.getDatabase(), jobGraphInfo.getSchema(), jobBean.getFromTb())
                .stream().collect(Collectors.toMap(DbTableField::getName, DbTableField::getType));

        if ("increment".equalsIgnoreCase(syncModeForm.getMode())) {
            transferSetting.setIncreaseFieldType(typeMappings.getOrDefault(syncModeForm.getIncreateField(), "string"));
            transferSetting.setIncreaseField(syncModeForm.getIncreateField());
            transferSetting.setStart(syncModeForm.getIncreateValue());
        }



        String selectField = jobConf.stream()
                .map(JobForm.FieldMappingForm::getSourceField)
                .filter(typeMappings::containsKey)
                .collect(Collectors.joining(", "));

        DatalinkXJobDetail.Reader reader = DatalinkXJobDetail.Reader
                .builder()
                .connectId(dsServiceImpl.getConnectId(fromDs))
                .type(jobGraphInfo.getType())
                .schema(jobGraphInfo.getDatabase())
                .transferSetting(transferSetting)
                .maxValue(syncModeForm.getIncreateValue())
                .tableName(jobBean.getFromTb())
                .columns(fromCols)
                .queryFields(selectField)
                .build();
        reader.setReaderGraph(dsReader.getReaderInfo(reader));
        return reader;
    }

    // 解析计算任务图
    private void analysisComputeGraph(DatalinkXJobDetail.SyncUnit syncUnit,
                                      String graph) {
        DatalinkXJobDetail.Compute compute = new DatalinkXJobDetail.Compute();
        if (ObjectUtils.isEmpty(graph)) {
            return;
        }

        boolean containSQLNode = false;
        List<DatalinkXJobDetail.Compute.Transform> transforms = new ArrayList<>();
        JsonNode jsonNode = JsonUtils.toJsonNode(graph);
        for (JsonNode node : jsonNode.get("cells")) {

            String transformType = node.get("shape").asText();
            if (MetaConstants.CommonConstant.TRANSFORM_SQL.equals(transformType)) {
                containSQLNode = true;
            }

            try {
                ITransformDriver transformDriver = ITransformFactory.getComputeDriver(transformType);

                if (!ObjectUtils.isEmpty(transformDriver)) {
                    transforms.add(
                            DatalinkXJobDetail
                                    .Compute
                                    .Transform
                                    .builder()
                                    .meta(transformDriver.analysisTransferMeta(node))
                                    .type(transformType)
                                    .build()
                    );
                }
            } catch (Exception ignored) {}
        }

        compute.setTransforms(transforms);

        // 如果计算过程中存在SQL节点，把reader中的queryFields改成*防止SQL中引用了未映射字段导致报错
//        if (containSQLNode) {
//            syncUnit.getReader().setQueryFields("*");
//        }

        syncUnit.setCommonSettings(new HashMap<String, Object>() {{
            put("openai.api_path", commonProperties.getOllamaUrl() + "/api/chat");
            put("model", commonProperties.getLlmModel());
            put("response_parse", commonProperties.getResponseParse());
            put("temperature", commonProperties.getTemperature());
            put("inner_prompt", commonProperties.getInnerPrompt());
        }});
        syncUnit.setCompute(compute);
    }

    @SneakyThrows
    private DatalinkXJobDetail.Writer getWriter(JobBean jobBean,
                                                List<JobForm.FieldMappingForm> jobConf) {

        DsBean toDs = dsRepository
                .findByDsId(jobBean.getWriterDsId())
                .orElseThrow(
                        () -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS, "to ds not exist")
                );

        JobDto.JobGraphDto jobGraphInfo = dsServiceImpl.getJobGraphInfo(toDs);
        List<String> toCols = jobConf
                .stream()
                .map(JobForm.FieldMappingForm::getTargetField)
                .collect(Collectors.toList());

        String insertFields = jobConf.stream()
                .map(JobForm.FieldMappingForm::getTargetField)
                .filter(targetField -> !ObjectUtils.isEmpty(targetField))
                .collect(Collectors.joining(", "));
        IDsWriter dsWriter = DsDriverFactory.getDsWriter(dsServiceImpl.getConnectId(toDs));


        DatalinkXJobDetail.Writer writer = DatalinkXJobDetail.Writer.builder()
                .schema(jobGraphInfo.getDatabase())
                .connectId(dsServiceImpl.getConnectId(toDs))
                .type(jobGraphInfo.getType())
                .insertFields(insertFields)
                .tableName(jobBean.getToTb())
                .columns(toCols)
                .build();
        writer.setWriterGraph(dsWriter.getWriterInfo(writer));
        return writer;
    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public String updateJobStatus(JobStateForm jobStateForm) {
        JobBean jobBean = jobRepository.findByJobId(jobStateForm.getJobId())
                .orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        int status = jobStateForm.getJobStatus();

        // 1、实时推送流转进度
        if (MetaConstants.JobType.JOB_TYPE_BATCH.equals(jobBean.getType())) {
            ProducerAdapterForm producerAdapterForm = new ProducerAdapterForm();
            producerAdapterForm.setType(MessageHubConstants.REDIS_STREAM_TYPE);
            producerAdapterForm.setTopic(MessageHubConstants.JOB_PROGRESS_TOPIC);
            producerAdapterForm.setGroup(MessageHubConstants.GLOBAL_COMMON_GROUP);
            producerAdapterForm.setMessage(
                    JsonUtils.toJson(
                            JobDto.StatusRefresh.builder()
                                    .status(status)
                                    .jobId(jobBean.getJobId())
                                    .build()
                    )
            );
            messageHubService.produce(producerAdapterForm);
        }

        // 2、存储流转任务状态
        JobDto.DataCountDto countVo = JobDto.DataCountDto.builder()
                .readCount(jobStateForm.getReadCount())
                .writeCount(jobStateForm.getWriteCount())
                .build();
        if (!ObjectUtils.isEmpty(jobStateForm.getStartTime())) {
            jobBean.setStartTime(new Timestamp(jobStateForm.getStartTime()));
        }
        jobBean.setStatus(status);
        jobBean.setCount(JsonUtils.toJson(countVo));
        jobBean.setErrorMsg(StringUtils.equalsIgnoreCase(jobStateForm.getErrmsg(), "success") ? "任务成功"
                : jobStateForm.getErrmsg() == null ? "" : jobStateForm.getErrmsg());

        if (!ObjectUtils.isEmpty(jobStateForm.getCheckpoint())) {
            jobBean.setCheckpoint(jobStateForm.getCheckpoint());
        }

        jobRepository.save(jobBean);

        // 3、保存流转任务执行日志
        JobLogBean jobLogBean = jobLogRepository.findByJobIdAndStartTime(jobBean.getJobId(), jobBean.getStartTime());
        if (ObjectUtils.isEmpty(jobLogBean)) {
            jobLogBean = JobLogBean.builder()
                    .jobId(jobStateForm.getJobId())
                    .status(ObjectUtils.nullSafeEquals(status, MetaConstants.JobStatus.JOB_STATUS_ERROR) ? 1 : 0)
                    .costTime(ObjectUtils.isEmpty(jobStateForm.getEndTime()) || ObjectUtils.isEmpty(jobBean.getStartTime()) ? 0 : (int) ((jobStateForm.getEndTime() - jobBean.getStartTime().getTime()) / 1000))
                    .errorMsg(StringUtils.equalsIgnoreCase(jobStateForm.getErrmsg(), "success") ? "任务成功" : jobStateForm.getErrmsg())
                    .count(JsonUtils.toJson(countVo))
                    .isDel(0)
                    .build();

            if (!ObjectUtils.isEmpty(jobStateForm.getStartTime())) {
                jobLogBean.setStartTime(new Timestamp(jobStateForm.getStartTime()));
            }
            if (!ObjectUtils.isEmpty(jobStateForm.getEndTime())) {
                jobLogBean.setEndTime(new Timestamp(jobStateForm.getEndTime()));
            }

            jobLogRepository.save(jobLogBean);
        } else {
            jobLogBean.setStatus(ObjectUtils.nullSafeEquals(status, MetaConstants.JobStatus.JOB_STATUS_ERROR) ? 1 : 0);
            jobLogBean.setCostTime(ObjectUtils.isEmpty(jobStateForm.getEndTime()) || ObjectUtils.isEmpty(jobBean.getStartTime()) ? 0 : (int) ((jobStateForm.getEndTime() - jobBean.getStartTime().getTime()) / 1000));
            jobLogBean.setErrorMsg(StringUtils.equalsIgnoreCase(jobStateForm.getErrmsg(), "success") ? "任务成功" : jobStateForm.getErrmsg());
            jobLogBean.setCount(JsonUtils.toJson(countVo));
            jobLogRepository.save(jobLogBean);
        }
        return jobBean.getJobId();
    }


    @Override
    public String updateJobTaskRel(String jobId, String taskId) {
        JobBean jobBean = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));

        jobBean.setTaskId(taskId);
        jobRepository.save(jobBean);
        return jobBean.getJobId();
    }

    @Override
    public void updateSyncMode(JobForm.SyncModifyForm syncMode) {
        JobBean jobBean = jobRepository.findByJobId(syncMode.getJobId())
                .orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
        JobForm.SyncModeForm syncModeForm = JsonUtils.toObject(jobBean.getSyncMode(), JobForm.SyncModeForm.class);
        syncModeForm.setIncreateValue(syncMode.getIncreateValue());
        jobBean.setSyncMode(JsonUtils.toJson(syncModeForm));
        jobRepository.save(jobBean);
    }

    @Override
    public String cascadeJob(String jobId) {
        jobRelationRepository.findSubJob(jobId).stream()
                .sorted(Comparator.comparingInt(JobRelationBean::getPriority))
                .forEach(jobRelationBean -> this.jobExec(jobRelationBean.getSubJobId()));
        return jobId;
    }

    public void jobExec(String jobId) {
        JobBean jobBean = jobRepository.findByJobId(jobId).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "任务不存在"));
        if (jobBean.getStatus() == JOB_STATUS_SYNCING) {
            throw new DatalinkXServerException(StatusCode.JOB_IS_RUNNING, "任务已在运行中，请勿重复触发");
        }

//		if (jobBean.getStatus() == JOB_STATUS_STOP) {
//			throw new DatalinkXServerException(StatusCode.SYNC_STATUS_ERROR, "任务处于停止状态");
//		}

        jobBean.setStatus(JOB_STATUS_QUEUE);
        jobBean.setStartTime(new Timestamp(System.currentTimeMillis()));

        // 如果xxl-job未创建任务，新建一个
        if (!jobClientApi.isXxljobExist(jobId)) {
            String xxlJobId = jobClientApi.add(jobBean.getCrontab(), jobId);
            jobBean.setXxlId(xxlJobId);
            jobClientApi.start(jobId);
        }

        jobRepository.save(jobBean);
        jobClientApi.trigger(jobId);
    }
}
