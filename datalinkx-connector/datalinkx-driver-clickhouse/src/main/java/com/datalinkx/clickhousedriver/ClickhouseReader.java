package com.datalinkx.clickhousedriver;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.driver.dsdriver.base.deserializer.EngineField;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcReader;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * ClickHouse Reader配置类
 * 用于支持 SeaTunnel ClickHouse Source 连接器
 * 参考文档：https://seatunnel.incubator.apache.org/zh-CN/docs/2.3.8/connector-v2/source/Clickhouse/
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ClickhouseReader extends JdbcReader {

    /**
     * ClickHouse 集群地址，格式是 host:port，允许多个 hosts 配置
     * 例如："host1:8123,host2:8123"
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String host;

    /**
     * ClickHouse 数据库名称
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String database;

    /**
     * 查询 SQL，用于通过 ClickHouse 服务器搜索数据
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String sql;

    /**
     * ClickHouse 用户账号
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String username;

    /**
     * ClickHouse 用户密码
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String password;

    /**
     * 可选参数，覆盖 clickhouse-jdbc 提供的所有参数
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("clickhouse.config")
    private Map<String, Object> clickhouseConfig;

    /**
     * 数据库服务器的会话时区
     * 默认值：ZoneId.systemDefault()
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("server_time_zone")
    private String serverTimeZone;
}