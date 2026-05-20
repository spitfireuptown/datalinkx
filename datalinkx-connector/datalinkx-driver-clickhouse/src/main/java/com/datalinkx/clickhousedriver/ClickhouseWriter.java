package com.datalinkx.clickhousedriver;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.driver.dsdriver.base.deserializer.EngineField;
import com.datalinkx.driver.dsdriver.jdbcdriver.JdbcWriter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * ClickHouse Writer配置类
 * 用于支持 SeaTunnel ClickHouse Sink 连接器
 * 参考文档：https://seatunnel.incubator.apache.org/zh-CN/docs/2.3.8/connector-v2/sink/Clickhouse/
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ClickhouseWriter extends JdbcWriter {

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
     * 表名称
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    private String table;

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
     * 每次通过 Clickhouse-jdbc 写入的行数
     * 默认值：20000
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("bulk_size")
    private String bulkSize = "20000";

    /**
     * 分片模式，此模式仅支持引擎为 Distributed 的 ClickHouse 表
     * 默认值：false
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("split_mode")
    private String splitMode = "false";

    /**
     * 使用 split_mode 时，指定分片算法的字段
     * 此选项仅在 split_mode 为 true 时有效
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("sharding_key")
    private String shardingKey;

    /**
     * 主键列，用于标记 ClickHouse 表中的主键列
     * 根据主键执行 INSERT/UPDATE/DELETE 操作
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("primary_key")
    private String primaryKey;

    /**
     * 是否支持更新插入
     * 用于 CDC 模式
     */
    @EngineField({EngineField.Engine.SEATUNNEL})
    @JsonProperty("support_upsert")
    private String supportUpsert;

    @EngineField({EngineField.Engine.SEATUNNEL})
    private String query;
}