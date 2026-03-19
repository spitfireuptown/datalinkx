package com.datalinkx.mysqldriver;


import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcDriver;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcReader;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcWriter;
import com.datalinkx.driver.dsdriver.setupinfo.MysqlSetupInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;


@Slf4j
public class MysqlDriver extends JdbcDriver<MysqlSetupInfo, JdbcReader, JdbcWriter> {

    public MysqlDriver(String connectId) {
        super(connectId);
    }

    private static final String MYSQL_DATABASE_JDBC_PATTERN = "jdbc:mysql://%s:%s/%s?useCursorFetch=%s"
            + "&useUnicode=%s&zeroDateTimeBehavior=%s&characterEncoding=%s&useInformationSchema=%s&serverTimezone=%s&socketTimeout=%s"
            + "&useSSL=false";

    private static final String MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";


    private static final Set<String> DATE_TYPE_SET = new HashSet<>();

    
    static {
        DATE_TYPE_SET.add("datetime");
        DATE_TYPE_SET.add("date");
        DATE_TYPE_SET.add("timestamp");
        DATE_TYPE_SET.add("time");
    }

    @Override
    protected String jdbcUrl() {
        return String.format(
                MYSQL_DATABASE_JDBC_PATTERN,
                this.jdbcSetupInfo.getServer(),
                this.jdbcSetupInfo.getPort(),
                this.jdbcSetupInfo.getDatabase(),
                this.jdbcSetupInfo.getUseCursorFetch(),
                this.jdbcSetupInfo.getUseUnicode(),
                this.jdbcSetupInfo.getZeroDateTimeBehavior(),
                this.jdbcSetupInfo.getCharacterEncoding(),
                this.jdbcSetupInfo.getUseInformationSchema(),
                this.jdbcSetupInfo.getServerTimezone(),
                this.jdbcSetupInfo.getSocketTimeout()
        );
    }
    @Override
    public String wrapValue(String fieldType, String value) {
        if (DATE_TYPE_SET.contains(fieldType.toLowerCase())) {

            return valueQuota() + value + valueQuota();
        } else {

            return value;
        }
    }


    @Override
    protected String driverClass() {
        return MYSQL_DRIVER_CLASS;
    }
}
