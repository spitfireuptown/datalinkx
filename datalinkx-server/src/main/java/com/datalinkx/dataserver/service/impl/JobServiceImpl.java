package com.datalinkx.dataserver.service.impl;


import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXServerException;
import com.datalinkx.common.result.StatusCode;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.dataserver.bean.domain.DsBean;
import com.datalinkx.dataserver.bean.domain.JobBean;
import com.datalinkx.dataserver.bean.domain.JobLogBean;
import com.datalinkx.dataserver.bean.domain.JobRelationBean;
import com.datalinkx.dataserver.bean.dto.JobDto;
import com.datalinkx.dataserver.bean.vo.JobVo;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.client.JobClientApi;
import com.datalinkx.dataserver.controller.form.JobForm;
import com.datalinkx.dataserver.repository.DsRepository;
import com.datalinkx.dataserver.repository.JobLogRepository;
import com.datalinkx.dataserver.repository.JobRepository;
import com.datalinkx.dataserver.service.JobRelationService;
import com.datalinkx.dataserver.service.JobService;
import com.datalinkx.dataserver.service.TransformService;
import com.datalinkx.driver.dsdriver.DsDriverFactory;
import com.datalinkx.driver.dsdriver.IDsReader;
import com.datalinkx.driver.dsdriver.IDsWriter;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.datalinkx.common.constants.MetaConstants.JobStatus.JOB_STATUS_STOP;
import static com.datalinkx.common.utils.IdUtils.genKey;
import static com.datalinkx.common.utils.JsonUtils.toJson;



@Service
@Log4j2
public class JobServiceImpl implements JobService {

	@Autowired
	JobRepository jobRepository;
	@Autowired
	JobRelationService jobRelationService;
	@Autowired
	DsServiceImpl dsServiceImpl;
	@Autowired
	DsRepository dsRepository;
	@Autowired
	JobLogRepository jobLogRepository;
	@Autowired
	JobClientApi jobClientApi;
	@Autowired
	TransformService transformService;


	@Transactional(rollbackFor = Exception.class)
	public String jobCreate(JobForm.JobCreateForm form) {
		this.validJobForm(form);
		String jobId = genKey("job");
		JobBean jobBean = new JobBean();
		jobBean.setJobId(jobId);

		jobBean.setReaderDsId(form.getFromDsId());
		jobBean.setWriterDsId(form.getToDsId());

		jobBean.setConfig(toJson(form.getFieldMappings()));
		jobBean.setFromTb(form.getFromTbName());
		jobBean.setToTb(form.getToTbName());
		jobBean.setStatus(MetaConstants.JobStatus.JOB_STATUS_CREATE);
		jobBean.setCrontab(form.getSchedulerConf());
		jobBean.setSyncMode(JsonUtils.toJson(form.getSyncMode()));
		jobBean.setName(form.getJobName());
		jobBean.setCover(form.getCover());
		jobBean.setGraph(form.getGraph());
		jobBean.setType(form.getType());

		// 创建 xxljob
		String xxlJobId = jobClientApi.add(form.getSchedulerConf(), jobId);

		jobBean.setXxlId(xxlJobId);
		jobRepository.save(jobBean);
		return jobId;
	}


	public String jobModify(JobForm.JobModifyForm form) {
		this.validJobForm(form);
		JobBean jobBean = jobRepository.findByJobId(form.getJobId()).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));
		jobBean.setReaderDsId(form.getFromDsId());
		jobBean.setWriterDsId(form.getToDsId());
		jobBean.setConfig(toJson(form.getFieldMappings()));
		jobBean.setFromTb(form.getFromTbName());
		jobBean.setToTb(form.getToTbName());
		jobBean.setCrontab(form.getSchedulerConf());
		jobBean.setSyncMode(JsonUtils.toJson(form.getSyncMode()));
		jobBean.setName(form.getJobName());
		jobBean.setCover(form.getCover());
		jobBean.setGraph(form.getGraph());
		jobRepository.save(jobBean);
		return form.getJobId();
	}

	// 校验计算任务transform graph是否合法
	private void validTransformGraph(DsBean fromDsBean, JobForm.JobCreateForm form) {
		if (ObjectUtils.isEmpty(form.getGraph())) {
			return;
		}

		Map<String, Integer> nodeBook = new HashMap<>();
		JsonNode jsonNode = JsonUtils.toJsonNode(form.getGraph());
		for (JsonNode node : jsonNode.get("cells")) {
			String nodeType = node.get("shape").asText();
			if ("edge".equals(nodeType)) {
				continue;
			}

			nodeBook.put(nodeType, nodeBook.getOrDefault(nodeType, 0) + 1);
		}

		// 目前只支持画布中三个节点, 前端太难写了，GPT都救不了，实在写不下去了。。。。
		if (nodeBook.keySet().size() > 3) {
			throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "仅支持单source输入节点、单输出sink节点、单transform节点");
		}

		// 目前只支持单source输入节点、单输出sink节点、单transform节点
		List<Integer> normalNodeNum = nodeBook.values().stream().filter(v -> v > 1).collect(Collectors.toList());
		if (!ObjectUtils.isEmpty(normalNodeNum)) {
			throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "仅支持单source输入节点、单输出sink节点、单transform节点");
		}

		// 如果来源数据源是http数据源，SQL算子无意义
		if (MetaConstants.DsType.DS_HTTP.equals(fromDsBean.getType()) && nodeBook.containsKey("sql")) {
			throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "HTTP数据源不支持SQL算子");
		}

		TransformNodeMeta.ValidateResult validate = transformService.validate(
				JobForm.TransformValidateForm
						.builder()
						.dsId(fromDsBean.getDsId())
						.graph(form.getGraph())
						.build()
		);
		if (!validate.isValid()) {
			throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, validate.getMessage());
		}
	}

	// 校验流转任务配置合法
	public void validJobForm(JobForm.JobCreateForm form) {
		// 1、判断数据源是否存在
		DsBean fromDsBean = dsRepository.findByDsId(form.getFromDsId()).orElseThrow(() -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS, "来源数据源不存在"));
		DsBean toDsBean = dsRepository.findByDsId(form.getToDsId()).orElseThrow(() -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS, "目标数据源不存在"));
		// 2、判断流转任务名称是否重复
		jobRepository.findByName(form.getJobName()).ifPresent(jobBean -> {
			if (form instanceof JobForm.JobModifyForm) {
				if (!ObjectUtils.nullSafeEquals(jobBean.getJobId(), ((JobForm.JobModifyForm) form).getJobId())) {
					throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "任务名称已存在");
				}
			} else {
				throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "任务名称已存在");
			}
		});

		// 3、判断增量模式下是否有增量字段
		if ((!ObjectUtils.isEmpty(form.getSyncMode()) && MetaConstants.JobSyncMode.INCREMENT_MODE.equals(form.getSyncMode().getMode()))
				&& ObjectUtils.isEmpty(form.getSyncMode().getIncreateField())) {
			throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "增量模式必须指定增量字段");
		}
		if (MetaConstants.JobSyncMode.INCREMENT_MODE.equals(form.getSyncMode().getMode())) {
			form.getSyncMode().setIncreateValue("");
		}
		// 4、判断增量模式下是否是时间类型或数值类型
		if ((!ObjectUtils.isEmpty(form.getSyncMode()) && MetaConstants.JobSyncMode.INCREMENT_MODE.equals(form.getSyncMode().getMode()))) {
			try {
				IDsReader dsReader = DsDriverFactory.getDsReader(dsServiceImpl.getConnectId(fromDsBean));
				Boolean isIncremental = dsReader.judgeIncrementalField(fromDsBean.getDatabase(), fromDsBean.getSchema(), form.getFromTbName(), form.getSyncMode().getIncreateField());
				if (!isIncremental) {
					throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "增量字段必须是日期或数值类型");
				}

				boolean isContainIncreaseField = form.getFieldMappings()
						.stream()
						.map(JobForm.FieldMappingForm::getSourceField)
						.collect(Collectors.toList())
						.contains(form.getSyncMode().getIncreateField());
				if (!isContainIncreaseField) {
					throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, "增量模式下同步字段中必须包含增量字段");
				}

			} catch (Exception e) {
				throw new DatalinkXServerException(StatusCode.JOB_CONFIG_ERROR, e.getMessage());
			}
		}
		// 6、校验计算任务transform graph是否合法
		this.validTransformGraph(fromDsBean, form);
	}


	// 创建任务后再手动调用xxl-job触发函数，解决异步调用mysql事务未提交，xxl-job任务已出发问题
	public String jobCreateProxy(JobForm.JobCreateForm form) {
		String jobId = this.jobCreate(form);
		// 1、创建后开启任务
		jobClientApi.start(jobId);
		// 2、默认触发一次任务
//		jobClientApi.trigger(jobId);
		return jobId;
	}

	public JobVo.JobInfoVo info(String jobId) {
		JobBean jobBean = jobRepository.findByJobId(jobId)
				.orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));


		JobVo.JobInfoVo jobInfoVo = JobVo.JobInfoVo
				.builder()
				.jobId(jobBean.getJobId())
				.jobName(jobBean.getName())
				.fromDsId(jobBean.getReaderDsId())
				.toDsId(jobBean.getWriterDsId())
				.fromTbName(jobBean.getFromTb())
				.toTbName(jobBean.getToTb())
				.schedulerConf(jobBean.getCrontab())
				.cover(jobBean.getCover())
				.graph(jobBean.getGraph())
				.syncMode(JsonUtils.toObject(jobBean.getSyncMode(), JobForm.SyncModeForm.class))
				.build();

		List<JobForm.FieldMappingForm> fieldMappingForms = JsonUtils.toList(jobBean.getConfig(), JobForm.FieldMappingForm.class);
		jobInfoVo.setFieldMappings(fieldMappingForms);
		return jobInfoVo;
	}

	public PageVo<List<JobVo.JobPageVo>> page(JobForm.JobPageForm form) {
		PageRequest pageRequest = PageRequest.of(form.getPageNo() - 1, form.getPageSize());
		Page<JobBean> jobBeans = jobRepository.pageQuery(pageRequest, form.getType(), form.getJobName());


		List<String> dsId = new ArrayList<>();
		jobBeans.getContent().forEach(job -> {
			dsId.add(job.getWriterDsId());
			dsId.add(job.getReaderDsId());
		});


		Map<String, String> dsNameMap = dsRepository.findAllByDsIdIn(dsId)
				.stream()
				.collect(Collectors.toMap(DsBean::getDsId, DsBean::getName));

		List<JobVo.JobPageVo> pageVoList = jobBeans.getContent().stream().map(jobBean -> {
			JobDto.DataCountDto dataCountDto = JsonUtils.toObject(jobBean.getCount(), JobDto.DataCountDto.class);
			return JobVo.JobPageVo
					.builder()
					.jobId(jobBean.getJobId())
					.jobName(jobBean.getName())
					.progress(String.format("%s/%s", dataCountDto.getWriteCount(), dataCountDto.getReadCount()))
					.fromTbName(dsNameMap.get(jobBean.getReaderDsId()) + "." + jobBean.getFromTb())
					.toTbName(dsNameMap.get(jobBean.getWriterDsId()) + "."  + jobBean.getToTb())
					.startTime(jobBean.getStartTime())
					.status(jobBean.getStatus())
					.build();
		}).collect(Collectors.toList());

		PageVo<List<JobVo.JobPageVo>> result = new PageVo<>();
		result.setPageNo(form.getPageNo());
		result.setPageSize(form.getPageSize());
		result.setData(pageVoList);
		result.setTotalPage(jobBeans.getTotalPages());
		result.setTotal(jobBeans.getTotalElements());
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public void del(String jobId, Boolean stream) {
		// 删除任务日志
		this.jobLogRepository.logicDeleteByJobId(jobId);

		// 流式任务没有使用xxl-job调度
		if (!stream) {
			// 删除任务调度配置
			this.jobClientApi.del(jobId);
		}
		// 删除任务依赖
		this.jobRelationService.del(jobId);
		// 删除任务
		this.jobRepository.logicDeleteByJobId(jobId);
	}

	public PageVo<List<JobVo.JobLogPageVo>> logPage(JobForm.JobLogPageForm jobLogPageForm) {
		PageRequest pageRequest = PageRequest.of(jobLogPageForm.getPageNo() - 1, jobLogPageForm.getPageSize());
		Page<JobLogBean> jobLogBeans = jobLogRepository.pageQuery(pageRequest, jobLogPageForm.getJobId());

		List<String> jobIds = jobLogBeans.getContent().stream().map(JobLogBean::getJobId).distinct().collect(Collectors.toList());
		Map<String, String> jobId2Name = jobRepository.findByJobIdIn(jobIds).stream().collect(Collectors.toMap(JobBean::getJobId, JobBean::getName));

		List<JobVo.JobLogPageVo> logPageVos = jobLogBeans.getContent().stream().map(jobLogBean -> {
			JobVo.JobLogPageVo logPageVo = new JobVo.JobLogPageVo();
			logPageVo.setJobId(jobLogBean.getJobId());
			logPageVo.setJobName(jobId2Name.get(jobLogBean.getJobId()));
			logPageVo.setStatus(jobLogBean.getStatus());
			logPageVo.setErrorMsg(jobLogBean.getErrorMsg());
			logPageVo.setCostTime(jobLogBean.getCostTime());
			logPageVo.setEndTime(jobLogBean.getEndTime());
			logPageVo.setStartTime(jobLogBean.getStartTime());


			JobDto.DataCountDto dataCountDto = JsonUtils.toObject(jobLogBean.getCount(), JobDto.DataCountDto.class);
			logPageVo.setAppendCount(dataCountDto.getReadCount());
			return logPageVo;
		}).collect(Collectors.toList());

		PageVo<List<JobVo.JobLogPageVo>> result = new PageVo<>();
		result.setPageNo(jobLogPageForm.getPageNo());
		result.setPageSize(jobLogPageForm.getPageSize());
		result.setData(logPageVos);
		result.setTotalPage(jobLogBeans.getTotalPages());
		result.setTotal(jobLogBeans.getTotalElements());
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public void jobStop(String jobId) {
		jobRepository.updateJobStatus(jobId, JOB_STATUS_STOP);
		jobClientApi.stop(jobId);
	}

	public List<JobVo.JobId2NameVo> list() {
		return jobRepository.findAll().stream().map(v -> JobVo.JobId2NameVo.builder().JobId(v.getJobId()).jobName(v.getName()).type(v.getType()).build()).collect(Collectors.toList());
	}


	@Transactional(rollbackFor = Exception.class)
	public String mcpDelByName(String name) {
		JobBean jobBean = jobRepository.findByName(name).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "流转任务不存在"));
		this.del(jobBean.getJobId(), MetaConstants.JobType.JOB_TYPE_STREAM.equals(jobBean.getType()));
		return jobBean.getJobId();
	}

	public String mcpJobExecByName(String name) {
		JobBean jobBean = jobRepository.findByName(name).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "流转任务不存在"));
		jobClientApi.trigger(jobBean.getJobId());
		return jobBean.getJobId();
	}

	public String mcpJobList() {
		return JsonUtils.toJson(this.list());
	}

	public String mcpJobInfo(String name) {
		JobBean jobBean = jobRepository.findByName(name)
				.orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "job not exist"));


		JobVo.JobInfoVo jobInfoVo = JobVo.JobInfoVo
				.builder()
				.jobId(jobBean.getJobId())
				.jobName(jobBean.getName())
				.fromDsId(jobBean.getReaderDsId())
				.toDsId(jobBean.getWriterDsId())
				.fromTbName(jobBean.getFromTb())
				.toTbName(jobBean.getToTb())
				.schedulerConf(jobBean.getCrontab())
				.cover(jobBean.getCover())
				.graph(jobBean.getGraph())
				.syncMode(JsonUtils.toObject(jobBean.getSyncMode(), JobForm.SyncModeForm.class))
				.build();

		List<JobForm.FieldMappingForm> fieldMappingForms = JsonUtils.toList(jobBean.getConfig(), JobForm.FieldMappingForm.class);
		jobInfoVo.setFieldMappings(fieldMappingForms);
		return JsonUtils.toJson(jobInfoVo);
	}

	public String mcpJobCascadeConfig(String jobName, String subJobName) {
		JobBean jobBean = jobRepository.findByName(jobName).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, jobName + "任务不存在"));
		JobBean subJobBean = jobRepository.findByName(subJobName).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, subJobName + "任务不存在"));

		JobForm.JobRelationForm jobRelationForm = new JobForm.JobRelationForm();
		jobRelationForm.setJobId(jobBean.getJobId());
		jobRelationForm.setSubJobId(subJobBean.getJobId());
		jobRelationForm.setPriority(0);

		jobRelationService.create(jobRelationForm);

		return "任务级联配置成功";
	}

	public String mcpJobCascadeDelete(String jobName) {
		JobBean jobBean = jobRepository.findByName(jobName).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, jobName + "任务不存在"));
		jobRelationService.delJobRelation(jobBean.getJobId());
		return jobName + "#任务级联删除成功";
	}
}