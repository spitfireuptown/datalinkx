package com.datalinkx.driver.dsdriver.base.transform;

import com.datalinkx.driver.dsdriver.transformdriver.TransformNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: uptown
 * @date: 2024/10/27 18:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
public class LLMNode extends TransformNode {

    @JsonProperty("model_provider")
    private String modelProvider;
    private String model;
    @JsonProperty("api_key")
    private String apiKey;
    private String prompt;
    @JsonProperty("openai.api_path")
    private String openaiApiPath;
    @JsonProperty("custom_config")
    private CustomConfig customConfig;

    @Data
    @Builder
    public static class CustomConfig {
        @JsonProperty("custom_response_parse")
        private String customResponseParse;
        @JsonProperty("custom_request_headers")
        @Builder.Default
        private Map<String, String> customRequestHeaders = new HashMap<String, String>() {{
            put("Content-Type", "application/json");
        }};
        @JsonProperty("custom_request_body")
        private customRequestBody customRequestBody;
    }

    @Data
    @Builder
    public static class customRequestBody {
        @Builder.Default
        private String model = "${model}";
        @Builder.Default
        private Double temperature = 0.1;
        @Builder.Default
        private Boolean stream = false;
        private List<Message> messages;
    }

    @Data
    @Builder
    public static class Message {
        @Builder.Default
        private String role = "user";
        private String content;
    }
}
