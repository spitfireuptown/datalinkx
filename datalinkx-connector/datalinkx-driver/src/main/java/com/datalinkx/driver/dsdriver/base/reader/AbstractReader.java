package com.datalinkx.driver.dsdriver.base.reader;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.driver.dsdriver.base.deserializer.EngineField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
public abstract class AbstractReader {
    public String engine = MetaConstants.CommonConstant.FLINKX_ENGINE;
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("plugin_name")
    private String pluginName;
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("source_table_name")
    private String sourceTableName;
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("result_table_name")
    private String resultTableName;
}
