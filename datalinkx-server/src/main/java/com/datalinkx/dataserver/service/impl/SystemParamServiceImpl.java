package com.datalinkx.dataserver.service.impl;

import com.datalinkx.common.exception.DatalinkXServerException;
import com.datalinkx.dataserver.bean.domain.SystemParamBean;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.SystemParamForm;
import com.datalinkx.dataserver.repository.SystemParamRepository;
import com.datalinkx.dataserver.service.SystemParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
public class SystemParamServiceImpl implements SystemParamService {

	@Autowired
	private SystemParamRepository systemParamRepository;


	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long create(SystemParamForm.SystemParamCreateForm form) {
		Optional<SystemParamBean> existing = systemParamRepository.findByParamKeyAndParamScope(form.getParamKey(), form.getParamScope());
		if (existing.isPresent()) {
			throw new DatalinkXServerException("参数key已存在: " + form.getParamKey());
		}

		SystemParamBean paramBean = SystemParamBean.builder()
				.paramKey(form.getParamKey())
				.paramValue(form.getParamValue())
				.paramDesc(form.getParamDesc())
				.paramScope(form.getParamScope())
				.enable(form.getEnable() != null ? form.getEnable() : true)
				.build();

		systemParamRepository.save(paramBean);
		return paramBean.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void modify(SystemParamForm.SystemParamCreateForm form) {
		SystemParamBean paramBean = systemParamRepository.findById(form.getId())
				.orElseThrow(() -> new DatalinkXServerException("参数不存在: " + form.getId()));

		paramBean.setParamValue(form.getParamValue());
		paramBean.setParamDesc(form.getParamDesc());
		paramBean.setParamScope(form.getParamScope());
		if (form.getEnable() != null) {
			paramBean.setEnable(form.getEnable());
		}

		systemParamRepository.save(paramBean);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void toggleEnable(Long id) {
		SystemParamBean paramBean = systemParamRepository.findById(id)
				.orElseThrow(() -> new DatalinkXServerException("参数不存在: " + id));

		paramBean.setEnable(!paramBean.getEnable());
		systemParamRepository.save(paramBean);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void del(Long id) {
		SystemParamBean paramBean = systemParamRepository.findById(id)
				.orElseThrow(() -> new DatalinkXServerException("参数不存在: " + id));

		systemParamRepository.deleteById(id);
	}

	@Override
	public SystemParamBean info(Long id) {
		return systemParamRepository.findById(id)
				.orElseThrow(() -> new DatalinkXServerException("参数不存在: " + id));
	}

	@Override
	public PageVo<List<SystemParamBean>> pageQuery(SystemParamForm.SystemParamPageForm form) {
		PageRequest pageRequest = PageRequest.of(form.getPageNo() - 1, form.getPageSize());
		Page<SystemParamBean> page = systemParamRepository.pageQuery(pageRequest, form.getParamKey(), form.getParamScope());

		PageVo<List<SystemParamBean>> result = new PageVo<>();
		result.setPageNo(form.getPageNo());
		result.setPageSize(form.getPageSize());
		result.setData(page.getContent());
		result.setTotalPage(page.getTotalPages());
		result.setTotal(page.getTotalElements());
		return result;
	}

	@Override
	public List<SystemParamBean> list() {
		return Optional.ofNullable(systemParamRepository.findAllByIsDel(0))
				.orElse(Collections.emptyList());
	}

	@Override
	public List<SystemParamBean> listByScope(String scope) {
		return Optional.ofNullable(systemParamRepository.findAllByParamScopeAndIsDel(scope, 0))
				.orElse(Collections.emptyList());
	}
}