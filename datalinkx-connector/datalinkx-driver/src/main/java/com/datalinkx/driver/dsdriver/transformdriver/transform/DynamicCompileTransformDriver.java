package com.datalinkx.driver.dsdriver.transformdriver.transform;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.driver.dsdriver.base.transform.DynamicCompileNode;
import com.datalinkx.driver.dsdriver.base.transform.TransformNode;
import com.datalinkx.driver.dsdriver.transformdriver.ITransformDriver;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DynamicCompileTransformDriver extends ITransformDriver {
    @Override
    public TransformNode transferInfo(Map<String, Object> commonSettings, String meta) {
        return DynamicCompileNode.builder()
                .pluginName(MetaConstants.CommonConstant.TRANSFORM_DYNAMIC_COMPILE)
                .sourceCode(meta)
                .compileLanguage(MetaConstants.CommonConstant.TRANSFORM_DYNAMIC_COMPILE_LANGUAGE)
                .sourceTableName(MetaConstants.CommonConstant.SOURCE_TABLE)
                .resultTableName(MetaConstants.CommonConstant.LLM_OUTPUT_TABLE)
                .build();
    }

    @Override
    public String analysisTransferMeta(JsonNode nodeMeta) {
        return "";
    }
}
