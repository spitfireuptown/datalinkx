package com.datalinkx.driver.dsdriver.transformdriver.transform;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.datalinkx.driver.dsdriver.base.transform.LLMNode;
import com.datalinkx.driver.dsdriver.base.transform.TransformNode;
import com.datalinkx.driver.dsdriver.transformdriver.ITransformDriver;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: uptown
 * @date: 2024/11/17 17:36
 */
@Slf4j
public class LLMTransformDriver extends ITransformDriver {

    @Override
    public TransformNode transferInfo(Map<String, Object> commonSettings, String meta) {
        Map<String, String> llmConfig = parseMeta(meta);
        
        String modelProvider = llmConfig.getOrDefault("modelProvider", "OPENAI");
        String model = llmConfig.getOrDefault("model", (String) commonSettings.get("model"));
        String apiKey = llmConfig.get("apiKey");
        String apiPath = llmConfig.get("apiPath");
        
        String innerPrompt = (String) commonSettings.getOrDefault("inner_prompt", "");
        String prompt = String.format("%s \n %s", innerPrompt, llmConfig.get("prompt"));

        LLMNode.Message promptMessage = LLMNode.Message.builder().content(prompt).build();
        
        LLMNode.LLMNodeBuilder builder = LLMNode.builder()
                .modelProvider(modelProvider)
                .pluginName(MetaConstants.CommonConstant.TRANSFORM_DYNAMIC_COMPILE_NAME)
                .model(model)
                .prompt(llmConfig.get("prompt"))
                .sourceTableName(MetaConstants.CommonConstant.SOURCE_TABLE)
                .resultTableName(MetaConstants.CommonConstant.LLM_OUTPUT_TABLE);

        if ("CUSTOM".equals(modelProvider)) {
            builder.customConfig(
                    LLMNode.CustomConfig.builder()
                            .customResponseParse((String) commonSettings.get("response_parse"))
                            .customRequestBody(
                                    LLMNode.customRequestBody
                                            .builder()
                                            .temperature(Double.valueOf((String) commonSettings.getOrDefault("temperature", "0.1")))
                                            .messages(Collections.singletonList(promptMessage))
                                            .build()
                            )
                            .build()
            ).apiKey((String) commonSettings.get("openai.api_path"));
        }

        if (StringUtils.isNotEmpty(apiKey)) {
            builder.apiKey(apiKey);
        }
        
        return builder.build();
    }

    @Override
    public String analysisTransferMeta(JsonNode nodeMeta) {
        JsonNode dataMeta = nodeMeta.get("data");
        Map<String, String> llmConfig = new HashMap<>();
        
        if (dataMeta.has("modelProvider")) {
            llmConfig.put("modelProvider", dataMeta.get("modelProvider").asText());
        }
        if (dataMeta.has("model")) {
            llmConfig.put("model", dataMeta.get("model").asText());
        }
        if (dataMeta.has("apiKey")) {
            llmConfig.put("apiKey", dataMeta.get("apiKey").asText());
        }
        if (dataMeta.has("apiPath")) {
            llmConfig.put("apiPath", dataMeta.get("apiPath").asText());
        }
        if (dataMeta.has("prompt")) {
            llmConfig.put("prompt", dataMeta.get("prompt").asText());
        }
        
        return JsonUtils.toJson(llmConfig);
    }

    @Override
    public TransformNodeMeta.ValidateResult verify(JsonNode nodeMeta) throws Exception {
        JsonNode dataMeta = nodeMeta.get("data");
        if (dataMeta == null) {
            throw new Exception("LLM配置数据为空");
        }
        
        if (!dataMeta.has("model") || StringUtils.isEmpty(dataMeta.get("model").asText())) {
            throw new Exception("模型名称不能为空");
        }
        
        if (!dataMeta.has("apiKey") || StringUtils.isEmpty(dataMeta.get("apiKey").asText())) {
            throw new Exception("API Key不能为空");
        }
        
        return null;
    }

    private Map<String, String> parseMeta(String meta) {
        if (StringUtils.isEmpty(meta)) {
            Map<String, String> defaultConfig = new HashMap<>();
            defaultConfig.put("prompt", "");
            return defaultConfig;
        }
        try {
            return JsonUtils.toObject(meta, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM meta, using default: {}", e.getMessage());
            Map<String, String> defaultConfig = new HashMap<>();
            defaultConfig.put("prompt", meta);
            return defaultConfig;
        }
    }
}
