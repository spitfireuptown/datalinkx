package com.datalinkx.dataserver.service;

import com.datalinkx.dataserver.bean.domain.SystemParamBean;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.SystemParamForm;

import java.util.List;

public interface SystemParamService {

	Long create(SystemParamForm.SystemParamCreateForm form);

	void modify(SystemParamForm.SystemParamCreateForm form);

	void del(Long id);

	void toggleEnable(Long id);

	SystemParamBean info(Long id);

	PageVo<List<SystemParamBean>> pageQuery(SystemParamForm.SystemParamPageForm form);

	List<SystemParamBean> list();

	List<SystemParamBean> listByScope(String scope);
}