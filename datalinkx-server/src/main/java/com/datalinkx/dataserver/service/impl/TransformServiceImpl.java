package com.datalinkx.dataserver.service.impl;

import com.datalinkx.dataserver.controller.form.JobForm;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.datalinkx.dataserver.service.TransformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态编译代码服务实现
 */
@Slf4j
@Service
public class TransformServiceImpl implements TransformService {

    @Override
    public TransformNodeMeta.ValidateResult validate(JobForm.TransformValidateForm validateForm) {
        return null;
    }
}
