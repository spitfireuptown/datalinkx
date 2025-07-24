package com.datalinkx.dataserver.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: uptown
 * @date: 2025/1/12 13:51
 */
@Configuration
@Data
public class CommonProperties {
    @Value("${data-transfer.fetch-size:1000}")
    Integer fetchSize;
    @Value("${data-transfer.stream-batch-size:10}")
    Integer streamBatchSize;
    @Value("${data-transfer.checkpoint-interval:6000}")
    Integer checkpointInterval;
    @Value("${data-transfer.checkpoint-path}")
    String checkpointPath;
    @Value("${data-transfer.query-time-out:10000}")
    Integer queryTimeOut;
    @Value("${client.ollama.url}")
    String ollamaUrl;
    @Value("${llm.model:}")
    String llmModel;
    @Value("${data-transfer.kafka-read-mode:group-offsets}")
    String kafkaReadMode;
    @Value("${llm.response_parse:}")
    String responseParse;
    @Value("${llm.temperature:0.1}")
    String temperature;
    @Value("${llm.inner_prompt:}")
    String innerPrompt;

    public Map<String, Object> commonSettingMap() {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                try {
                    field.setAccessible(true);
                    map.put(field.getName(), field.get(this));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return map;
    }
}
