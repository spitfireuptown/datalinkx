package com.datalinkx.httpdriver;

import com.datalinkx.driver.dsdriver.base.reader.AbstractReader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class HttpReader extends AbstractReader {
    private String url;
    private String method;
    @Builder.Default
    private String format = "json";
    private Map<String, Object> params;
    private HttpReader.Schema schema;
    @JsonProperty("json_field")
    private Map<String, String> jsonField;
    private Map<String, Object> headers;
    private String body;

    @Data
    public static final class Schema {
        // 为什么用LinkedHashMap？ 因为要保证写入顺序与页面上配置的字段映射顺序一致
        private LinkedHashMap<String, String> fields;
    }
}
