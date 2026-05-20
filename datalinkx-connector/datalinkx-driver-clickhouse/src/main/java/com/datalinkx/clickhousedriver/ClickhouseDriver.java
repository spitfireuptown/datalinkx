package com.datalinkx.clickhousedriver;


import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcDriver;
import com.datalinkx.driver.dsdriver.setupinfo.JdbcSetupInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Slf4j
public class ClickhouseDriver extends JdbcDriver<JdbcSetupInfo, ClickhouseReader, ClickhouseWriter> {


    public ClickhouseDriver(String connectId) {
        super(connectId);
        this.PLUGIN_NAME = "Clickhouse";
    }

    private static final String CLICKHOUSE_DATABASE_JDBC_PATTERN = "jdbc:clickhouse://%s:%s/%s";
    private static final String CLICKHOUSE_DRIVER_CLASS = "ru.yandex.clickhouse.ClickHouseDriver";
    private static final Set<String> DATE_TYPE_SET = new HashSet<>();

    static {
        DATE_TYPE_SET.add("datetime");
        DATE_TYPE_SET.add("date");
        DATE_TYPE_SET.add("timestamp");
        DATE_TYPE_SET.add("time");
    }

    @Override
    protected String driverClass() {
        return CLICKHOUSE_DRIVER_CLASS;
    }

    @Override
    protected String jdbcUrl() {
        return String.format(
                CLICKHOUSE_DATABASE_JDBC_PATTERN,
                this.jdbcSetupInfo.getServer(),
                this.jdbcSetupInfo.getPort(),
                this.jdbcSetupInfo.getDatabase()
        );
    }

    @Override
    public ClickhouseReader getSourceInfo(DatalinkXJobDetail.Reader reader) {
        return ClickhouseReader.builder()
                .engine(MetaConstants.CommonConstant.SEATUNNEL_ENGINE)
                .host(this.jdbcSetupInfo.getServer() + ":" + this.jdbcSetupInfo.getPort())
                .database(this.jdbcSetupInfo.getDatabase())
                .sql(this.transferSourceSQL(reader))
                .username(this.jdbcSetupInfo.getUid())
                .password(this.jdbcSetupInfo.getPwd())
                .pluginName(this.PLUGIN_NAME)
                .resultTableName(MetaConstants.CommonConstant.SOURCE_TABLE)
                .build();
    }

    @Override
    public String transferSourceSQL(DatalinkXJobDetail.Reader reader) {
        String sourceSQL = String.format("select %s from %s.%s", reader.getQueryFields(), reader.getSchema(), reader.getTableName());
        try {
            String increaseSQL = this.genWhere(reader);
            if (increaseSQL != null && !increaseSQL.isEmpty()) {
                sourceSQL += String.format(" where %s", increaseSQL);
            }
        } catch (Exception e) {
            log.error("gen increase sql error: " + e.getMessage(), e);
            throw new RuntimeException("生成增量条件 SQL 失败", e);
        }

        return sourceSQL;
    }

    @Override
    public ClickhouseWriter getSinkInfo(DatalinkXJobDetail.Writer writer) {
        return ClickhouseWriter.builder()
                .engine(MetaConstants.CommonConstant.SEATUNNEL_ENGINE)
                .host(this.jdbcSetupInfo.getServer() + ":" + this.jdbcSetupInfo.getPort())
                .database(this.jdbcSetupInfo.getDatabase())
                .table(writer.getTableName())
                .username(this.jdbcSetupInfo.getUid())
                .password(this.jdbcSetupInfo.getPwd())
                .query(super.transferSinkSQL(writer))
                .pluginName(this.PLUGIN_NAME)
                .bulkSize("20000")
                .splitMode("false")
                .resultTableName(MetaConstants.CommonConstant.RESULT_TABLE)
                .build();
    }
}
