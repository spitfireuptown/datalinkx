package com.datalinkx.dataserver.controller;

import com.datalinkx.common.result.WebResult;
import com.datalinkx.dataserver.bean.vo.JobVo;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.JobForm;
import com.datalinkx.dataserver.service.DtsJobService;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.datalinkx.dataserver.service.TransformService;
import com.datalinkx.dataserver.service.impl.JobRelationServiceImpl;
import com.datalinkx.dataserver.service.impl.JobServiceImpl;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/job")
public class JobController {

	@Autowired
	private JobServiceImpl jobServiceImpl;

	@Autowired
	private DtsJobService dtsJobService;

	@Autowired
	private JobRelationServiceImpl jobRelationServiceImpl;

	@Autowired
	private TransformService transformService;

	@ApiOperation("流转任务-创建")
	@PostMapping("/create")
	public WebResult<String> jobCreate(@RequestBody JobForm.JobCreateForm form) {
		return WebResult.of(jobServiceImpl.jobCreateProxy(form));
	}

	@ApiOperation("流转任务-编辑")
	@PostMapping("/modify")
	public WebResult<String> jobModify(@RequestBody JobForm.JobModifyForm form) {
		return WebResult.of(jobServiceImpl.jobModify(form));
	}


	@ApiOperation("流转任务-删除")
	@PostMapping("/delete/{jobId}")
	public WebResult<String> delete(@PathVariable String jobId) {
		jobServiceImpl.del(jobId, false);
		return WebResult.of(jobId);
	}

	@ApiOperation("流转任务-列表查询")
    @GetMapping("/page")
    public PageVo<List<JobVo.JobPageVo>> page(JobForm.JobPageForm form) {
        return jobServiceImpl.page(form);
    }

	@ApiOperation("流转任务-列表查询")
	@GetMapping("/list")
	public WebResult<List<JobVo.JobId2NameVo>> list() {
		return WebResult.of(jobServiceImpl.list());
	}

	@ApiOperation("流转任务-详情")
	@GetMapping("/info/{jobId}")
	public WebResult<JobVo.JobInfoVo> info(@PathVariable String jobId) {
		return WebResult.of(jobServiceImpl.info(jobId));
	}

	@ApiOperation("流转任务-手动执行")
	@RequestMapping("/exec/{jobId}")
	public WebResult<String> jobExec(@PathVariable String jobId) {
		dtsJobService.jobExec(jobId);
		return WebResult.of(jobId);
	}

	@ApiOperation("流转任务-暂停")
	@RequestMapping("/stop/{jobId}")
	public WebResult<String> jobStop(@PathVariable String jobId) {
		jobServiceImpl.jobStop(jobId);
		return WebResult.of(jobId);
	}

	@ApiOperation("流转任务执行日志-分页查询")
	@RequestMapping("/log/page")
	public PageVo<List<JobVo.JobLogPageVo>> logPage(JobForm.JobLogPageForm jobLogPageForm) {
		return jobServiceImpl.logPage(jobLogPageForm);
	}

	@ApiOperation("任务依赖-配置分页查询")
	@RequestMapping("/relation/page")
	public PageVo<List<JobVo.JobRelationVo>> relationPage(JobForm.JobRelationPageForm jobRelationPageForm) {
		return jobRelationServiceImpl.page(jobRelationPageForm);
	}


	@ApiOperation("流转任务-创建")
	@PostMapping("/relation/create")
	public WebResult<String> relationCreate(@RequestBody JobForm.JobRelationForm form) {
		return WebResult.of(jobRelationServiceImpl.create(form));
	}

	@ApiOperation("任务依赖-删除配置")
	@PostMapping("/delete_relation/{relationId}")
	public WebResult<String> deleteRelation(@PathVariable String relationId) {
		jobRelationServiceImpl.del(relationId);
		return WebResult.of(relationId);
	}


	@ApiOperation("任务血缘-详情查询")
    @RequestMapping("/relation_blood/info/{jobId}")
    public JobVo.JobRelationBloodVo relationPage(@PathVariable String jobId) {
        return jobRelationServiceImpl.relationBloodInfo(jobId);
    }

	@ApiOperation("校验动态编译源代码")
	@PostMapping("/validate_transform_meta")
	public WebResult<TransformNodeMeta.ValidateResult> validateTransformMeta(@RequestBody JobForm.TransformValidateForm form) {
		return WebResult.of(transformService.validate(form));
	}

}
