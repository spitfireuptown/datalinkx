package com.datalinkx.driver.dsdriver.jdbcdriver;

import java.util.List;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.driver.dsdriver.base.column.WriterConnection;
import com.datalinkx.driver.dsdriver.base.deserializer.EngineField;
import com.datalinkx.driver.dsdriver.base.writer.AbstractWriter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class JdbcWriter extends AbstractWriter {
    public String engine = MetaConstants.CommonConstant.FLINKX_ENGINE;
    @EngineField({EngineField.Engine.FLINKX})
    String username;
    @EngineField({EngineField.Engine.FLINKX, EngineField.Engine.SEATUNNEL})
    String password;
    @EngineField({EngineField.Engine.FLINKX})
    List<String> column;
    @EngineField({EngineField.Engine.FLINKX})
    List<WriterConnection> connection;
    @EngineField({EngineField.Engine.FLINKX})
    String writeMode;
    @EngineField({EngineField.Engine.FLINKX})
    String insertSqlMode;
    @EngineField({EngineField.Engine.FLINKX})
    List<String> preSql;

    // seatunnel engine writer info
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String url;
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String driver;
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String user;
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String query;
}
