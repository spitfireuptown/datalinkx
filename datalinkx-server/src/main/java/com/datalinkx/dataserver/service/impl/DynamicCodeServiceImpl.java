package com.datalinkx.dataserver.service.impl;

import com.datalinkx.dataserver.bean.vo.DynamicCodeVo;
import com.datalinkx.dataserver.service.DynamicCodeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class DynamicCodeServiceImpl implements DynamicCodeService {

    @Override
    public DynamicCodeVo.ValidateResult validateAndParse(String sourceCode) {
        log.info("开始校验动态编译源代码");

        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return DynamicCodeVo.ValidateResult.builder()
                    .valid(false)
                    .message("源代码不能为空")
                    .outputFields(new ArrayList<>())
                    .build();
        }

        // 检查是否包含必要的方法
        if (!sourceCode.contains("getInlineOutputColumns")) {
            return DynamicCodeVo.ValidateResult.builder()
                    .valid(false)
                    .message("源代码必须包含 getInlineOutputColumns 方法")
                    .outputFields(new ArrayList<>())
                    .build();
        }

        if (!sourceCode.contains("getInlineOutputFieldValues")) {
            return DynamicCodeVo.ValidateResult.builder()
                    .valid(false)
                    .message("源代码必须包含 getInlineOutputFieldValues 方法")
                    .outputFields(new ArrayList<>())
                    .build();
        }

        // 解析 getInlineOutputColumns 方法返回的字段
        List<DynamicCodeVo.OutputField> outputFields = parseOutputColumns(sourceCode);

        return DynamicCodeVo.ValidateResult.builder()
                .valid(true)
                .message("语法校验通过")
                .outputFields(outputFields)
                .build();
    }

    /**
     * 解析 getInlineOutputColumns 方法中的 PhysicalColumn 定义
     */
    private List<DynamicCodeVo.OutputField> parseOutputColumns(String sourceCode) {
        List<DynamicCodeVo.OutputField> fields = new ArrayList<>();

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

            fields.add(DynamicCodeVo.OutputField.builder()
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
