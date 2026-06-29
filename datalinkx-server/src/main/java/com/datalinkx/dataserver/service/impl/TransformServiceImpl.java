package com.datalinkx.dataserver.service.impl;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.dataserver.bean.domain.DsBean;
import com.datalinkx.dataserver.controller.form.JobForm;
import com.datalinkx.dataserver.service.DsService;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.datalinkx.dataserver.service.TransformService;
import com.datalinkx.driver.dsdriver.transformdriver.ITransformDriver;
import com.datalinkx.driver.dsdriver.transformdriver.ITransformFactory;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态编译代码服务实现
 */
@Slf4j
@Service
public class TransformServiceImpl implements TransformService {

    @Autowired
    DsService dsService;

    @Override
    public TransformNodeMeta.ValidateResult validate(JobForm.TransformValidateForm validateForm) {

        if (ObjectUtils.isEmpty(validateForm.getDsId())) {
            return TransformNodeMeta.ValidateResult.builder().valid(false).message("未配置来源数据源").build();
        }

        DsBean fromDsBean = dsService.info(validateForm.getDsId());
        String connectId = dsService.getConnectId(fromDsBean);

        JsonNode completeMeta = JsonUtils.toJsonNode(validateForm.getGraph());
        for (JsonNode node : completeMeta.get("cells")) {
            String nodeType = node.get("shape").asText();
            if (Arrays.asList(
                    MetaConstants.CommonConstant.TRANSFORM_EDGE,
                    MetaConstants.CommonConstant.TRANSFORM_START,
                    MetaConstants.CommonConstant.TRANSFORM_END
            ).contains(nodeType)) {
                continue;
            }

            try {
                ITransformDriver transformDriver = ITransformFactory.getComputeDriver(nodeType);
                return transformDriver.verify(connectId, completeMeta);
            } catch (Exception e) {
                log.error("{} 算子反射异常", nodeType, e);
                return TransformNodeMeta.ValidateResult.builder().valid(false).message(e.getMessage()).build();
            }
        }
        return TransformNodeMeta.ValidateResult.builder().valid(true).message("算子校验成功").build();
    }
}
