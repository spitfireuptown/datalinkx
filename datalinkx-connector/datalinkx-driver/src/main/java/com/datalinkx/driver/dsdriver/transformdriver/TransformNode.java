package com.datalinkx.driver.dsdriver.transformdriver;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author: uptown
 * @date: 2024/10/27 19:33
 */
@SuperBuilder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransformNode {
    @JsonProperty("plugin_name")
    private String pluginName;
    @JsonProperty("source_table_name")
    private String sourceTableName;
    @JsonProperty("result_table_name")
    private String resultTableName;
}
