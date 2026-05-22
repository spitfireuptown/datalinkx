package com.datalinkx.dataserver.controller;

import com.datalinkx.common.result.WebResult;
import com.datalinkx.dataserver.bean.domain.SystemParamBean;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.SystemParamForm;
import com.datalinkx.dataserver.service.impl.SystemParamServiceImpl;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/system/param")
@Api(tags = "systemParam")
public class SystemParamController {

	@Resource
	private SystemParamServiceImpl systemParamServiceImpl;

	@GetMapping("/page")
	public PageVo<List<SystemParamBean>> pageQuery(SystemParamForm.SystemParamPageForm form) {
		return systemParamServiceImpl.pageQuery(form);
	}

	@GetMapping("/list")
	public WebResult<List<SystemParamBean>> list() {
		return WebResult.of(systemParamServiceImpl.list());
	}

	@GetMapping("/list/scope")
	public WebResult<List<SystemParamBean>> listByScope(@RequestParam String scope) {
		return WebResult.of(systemParamServiceImpl.listByScope(scope));
	}

	@GetMapping("/info/{id}")
	public WebResult<SystemParamBean> info(@PathVariable Long id) {
		return WebResult.of(systemParamServiceImpl.info(id));
	}

	@PostMapping("/create")
	public WebResult<Long> create(@RequestBody SystemParamForm.SystemParamCreateForm form) {
		return WebResult.of(systemParamServiceImpl.create(form));
	}

	@PostMapping("/modify")
	public void modify(@RequestBody SystemParamForm.SystemParamCreateForm form) {
		systemParamServiceImpl.modify(form);
	}

	@PostMapping("/delete/{id}")
	public void del(@PathVariable Long id) {
		systemParamServiceImpl.del(id);
	}

	@PostMapping("/toggle/{id}")
	public void toggleEnable(@PathVariable Long id) {
		systemParamServiceImpl.toggleEnable(id);
	}
}