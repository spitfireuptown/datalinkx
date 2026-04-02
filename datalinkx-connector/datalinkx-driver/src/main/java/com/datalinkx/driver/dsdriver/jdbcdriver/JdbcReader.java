package com.datalinkx.driver.dsdriver.jdbcdriver;

import com.datalinkx.driver.dsdriver.base.column.ReaderConnection;
import com.datalinkx.driver.dsdriver.base.deserializer.EngineField;
import com.datalinkx.driver.dsdriver.base.reader.AbstractReader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
//@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class JdbcReader extends AbstractReader {
    @EngineField({EngineField.Engine.FLINKX})
    String username;
    @EngineField({EngineField.Engine.FLINKX, EngineField.Engine.SEATUNNEL})
    String password;

    @EngineField({EngineField.Engine.FLINKX})
    String where;
    @EngineField({EngineField.Engine.FLINKX})
    int fetchSize;
    @EngineField({EngineField.Engine.FLINKX})
    int queryTimeOut;

    @EngineField({EngineField.Engine.FLINKX})
    List<String> column;
    @EngineField({EngineField.Engine.FLINKX})
    List<ReaderConnection> connection;



    // seatunnel engine reader info
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String url;
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String driver;
    @EngineField({EngineField.Engine.SEATUNNEL})
    @Builder.Default
    @JsonProperty("connection_check_timeout_sec")
    private Integer connectionCheckTimeoutSec = 60;
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String user;
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String query;
    @EngineField({EngineField.Engine.SEATUNNEL})
    @Builder.Default
    @JsonProperty("split.size")
    private Integer splitSize = 1000;
}
