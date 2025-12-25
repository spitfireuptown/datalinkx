package com.datalinkx.clickhousedriver;


import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcDriver;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcReader;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcWriter;
import com.datalinkx.driver.dsdriver.setupinfo.JdbcSetupInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Slf4j
public class ClickhouseDriver extends JdbcDriver<JdbcSetupInfo, JdbcReader, JdbcWriter> {


    public ClickhouseDriver(String connectId) {
        Map customConfig = JsonUtils.toObject(ConnectIdUtils.decodeConnectId(connectId), Map.class);
        log.info("inject custom config:{}", customConfig);
        Map innerConfig = JsonUtils.toObject((String) customConfig.get("config"), Map.class);
        log.info("inject custom inner config:{}", innerConfig);
        jdbcSetupInfo = new JdbcSetupInfo();
        jdbcSetupInfo.setPwd((String) innerConfig.get("pwd"));
        jdbcSetupInfo.setServer((String) innerConfig.get("server"));
        jdbcSetupInfo.setDatabase((String) innerConfig.get("database"));
        jdbcSetupInfo.setUid((String) innerConfig.get("uid"));
        jdbcSetupInfo.setPort((Integer) innerConfig.get("port"));

        this.connectId = connectId;
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
}
