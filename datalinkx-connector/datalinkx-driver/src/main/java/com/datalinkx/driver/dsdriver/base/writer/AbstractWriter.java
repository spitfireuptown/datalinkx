package com.datalinkx.driver.dsdriver.base.writer;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.driver.dsdriver.base.deserializer.EngineField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class AbstractWriter {
    public String engine = MetaConstants.CommonConstant.FLINKX_ENGINE;
    @EngineField({EngineField.Engine.FLINKX})
    Integer batchSize;
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
