package com.datalinkx.dataserver.service;

import com.datalinkx.dataserver.controller.form.JobForm;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;

/**
 * 动态编译代码服务
 */
public interface TransformService {

    TransformNodeMeta.ValidateResult validate(JobForm.TransformValidateForm validateForm);

}
