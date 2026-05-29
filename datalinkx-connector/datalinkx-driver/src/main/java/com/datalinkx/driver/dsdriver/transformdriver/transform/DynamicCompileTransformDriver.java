package com.datalinkx.driver.dsdriver.transformdriver.transform;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.datalinkx.driver.dsdriver.base.transform.DynamicCompileNode;
import com.datalinkx.driver.dsdriver.base.transform.TransformNode;
import com.datalinkx.driver.dsdriver.transformdriver.ITransformDriver;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DynamicCompileTransformDriver extends ITransformDriver {
    @Override
    public TransformNode transferInfo(Map<String, Object> commonSettings, String meta) {
        return DynamicCompileNode.builder()
                .pluginName(MetaConstants.CommonConstant.TRANSFORM_DYNAMIC_COMPILE_NAME)
                .sourceCode(meta)
                .compileLanguage(MetaConstants.CommonConstant.TRANSFORM_DYNAMIC_COMPILE_LANGUAGE)
                .compilePattern(MetaConstants.CommonConstant.TRANSFORM_DYNAMIC_COMPILE_PATTERN)
                .sourceTableName(MetaConstants.CommonConstant.SOURCE_TABLE)
                .resultTableName(MetaConstants.CommonConstant.DYNAMIC_COMPILE_OUTPUT_TABLE)
                .build();
    }

    @Override
    public String analysisTransferMeta(JsonNode nodeMeta) {
        JsonNode dataMeta = nodeMeta.get("data");
        return dataMeta.get("sourceCode").asText();
    }

    @Override
    public TransformNodeMeta.ValidateResult verify(String connectId, JsonNode nodeMeta) {
        JsonNode dataMeta = findNodeDataByType(nodeMeta, "DynamicCompile");
        String sourceCode = null;
        
        if (dataMeta != null && dataMeta.has("sourceCode")) {
            sourceCode = dataMeta.get("sourceCode").asText();
        }
        
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return TransformNodeMeta.ValidateResult.builder()
                    .valid(false)
                    .message("源代码不能为空")
                    .outputFields(new ArrayList<>())
                    .build();
        }

        // 检查是否包含必要的方法
        if (!sourceCode.contains("getInlineOutputColumns")) {
            return TransformNodeMeta.ValidateResult.builder()
                    .valid(false)
                    .message("源代码必须包含 getInlineOutputColumns 方法")
                    .outputFields(new ArrayList<>())
                    .build();
        }

        if (!sourceCode.contains("getInlineOutputFieldValues")) {
            return TransformNodeMeta.ValidateResult.builder()
                    .valid(false)
                    .message("源代码必须包含 getInlineOutputFieldValues 方法")
                    .outputFields(new ArrayList<>())
                    .build();
        }

        // 解析 getInlineOutputColumns 方法返回的字段
        List<TransformNodeMeta.OutputField> outputFields = parseOutputColumns(sourceCode);

        return TransformNodeMeta.ValidateResult.builder()
                .valid(true)
                .message("语法校验通过")
                .outputFields(outputFields)
                .build();
    }


    /**
     * 解析 getInlineOutputColumns 方法中的 PhysicalColumn 定义
     */
    private List<TransformNodeMeta.OutputField> parseOutputColumns(String sourceCode) {
        List<TransformNodeMeta.OutputField> fields = new ArrayList<>();

        // 匹配 PhysicalColumn.of 调用
        Pattern pattern = Pattern.compile(
                "PhysicalColumn\\.of\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*(\\w+)"
        );
        Matcher matcher = pattern.matcher(sourceCode);

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String fieldType = matcher.group(2);

            // 转换类型为简化形式
            String simplifiedType = simplifyType(fieldType);

            fields.add(TransformNodeMeta.OutputField.builder()
                    .name(fieldName)
                    .type(simplifiedType)
                    .build());
        }

        return fields;
    }

    /**
     * 简化类型名称
     */
    private String simplifyType(String type) {
        if (type.contains("STRING")) {
            return "STRING";
        } else if (type.contains("INT") && !type.contains("BIGINT")) {
            return "INT";
        } else if (type.contains("BIGINT")) {
            return "BIGINT";
        } else if (type.contains("DOUBLE") || type.contains("FLOAT") || type.contains("DECIMAL")) {
            return "DOUBLE";
        } else if (type.contains("BOOLEAN")) {
            return "BOOLEAN";
        } else if (type.contains("DATE") || type.contains("TIME")) {
            return "DATE";
        }
        return "STRING";
    }
}
