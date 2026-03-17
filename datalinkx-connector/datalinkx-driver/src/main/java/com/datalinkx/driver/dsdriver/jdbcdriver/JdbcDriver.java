package com.datalinkx.driver.dsdriver.jdbcdriver;


import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXJobException;
import com.datalinkx.common.exception.DatalinkXServerException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.common.utils.TelnetUtil;
import com.datalinkx.compute.connector.jdbc.JdbcSink;
import com.datalinkx.compute.connector.jdbc.JdbcSource;
import com.datalinkx.compute.connector.jdbc.TransformNode;
import com.datalinkx.driver.dsdriver.IDsDriver;
import com.datalinkx.driver.dsdriver.IDsReader;
import com.datalinkx.driver.dsdriver.IDsWriter;
import com.datalinkx.driver.dsdriver.base.AbstractDriver;
import com.datalinkx.driver.dsdriver.base.column.ReaderConnection;
import com.datalinkx.driver.dsdriver.base.column.WriterConnection;
import com.datalinkx.driver.dsdriver.base.meta.DbTableField;
import com.datalinkx.driver.dsdriver.base.reader.ReaderInfo;
import com.datalinkx.driver.dsdriver.base.writer.WriterInfo;
import com.datalinkx.driver.dsdriver.setupinfo.JdbcSetupInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Slf4j
public class JdbcDriver<T extends JdbcSetupInfo, P extends JdbcReader, Q extends JdbcWriter>
        extends AbstractDriver<T, P, Q> implements IDsDriver, IDsReader, IDsWriter {

    protected T jdbcSetupInfo;
    protected String connectId;
    protected String PLUGIN_NAME = "Jdbc";


    public JdbcDriver(String connectId) {
        Class clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.jdbcSetupInfo = (T) JsonUtils.toObject(ConnectIdUtils.decodeConnectId(connectId), clazz);
        jdbcSetupInfo.setPwd(rebuildPassword(jdbcSetupInfo.getPwd()));
        this.connectId = connectId;
    }

    public JdbcDriver() {
    }

    protected String jdbcUrl() {
        return "";
    }


    protected String driverClass() {
        return "";
    }

    protected Properties connectProp() {
        Properties info = new Properties();

        if (jdbcSetupInfo.getUid() != null) {
            info.put("user", jdbcSetupInfo.getUid());
        }
        if (jdbcSetupInfo.getPwd() != null) {
            info.put("password", jdbcSetupInfo.getPwd());
        }
        return info;
    }

    @Override
    public Object connect(boolean check) throws Exception {
        Connection connection = null;
        Driver injectDriver = null;

        String url = jdbcUrl();
        String errorMsg;
        try {
            if (ObjectUtils.isEmpty(urlClassLoader)) {

                Class.forName(driverClass()).getDeclaredConstructor().newInstance();
            } else {

                injectDriver = (Driver) Class.forName(driverClass(), true, urlClassLoader).getDeclaredConstructor().newInstance();
            }
        } catch (ClassNotFoundException e) {
            errorMsg = "dsdriver class not exist";
            log.error(errorMsg, e);
            throw new Exception(errorMsg);
        }

        TelnetUtil.telnet(this.jdbcSetupInfo.getServer(), this.jdbcSetupInfo.getPort());

        try {
            connection = DriverManager.getConnection(url, connectProp());
        } catch (Exception e) {
            log.warn("默认加载器未加载驱动，使用自定义加载器" );
            if (!ObjectUtils.isEmpty(urlClassLoader) && !ObjectUtils.isEmpty(injectDriver)) {
                connection = injectDriver.connect(url, connectProp());
            }
        }

        if (connection == null) {
            throw new DatalinkXServerException("数据库连接失败, 请检查日志");
        }

        if (check) {
            connection.close();
        }

        return connection;
    }


    public List<String> treeTable(String catalog, String schema) throws Exception {
        Connection connection = (Connection) this.connect(false);
        try {

            return generateTree(catalog, schema, connection);
        } finally {

            this.disConnect(connection);
        }
    }

    @Override
    public List<DbTableField> getFields(String catalog, String schema, String tableName) throws Exception {
        Connection connection = (Connection) this.connect(false);
        List<Map<String, Object>> maps = fetchColumn(catalog, schema, tableName, connection);
        return JsonUtils.toList(JsonUtils.toJson(maps), DbTableField.class);
    }


    public List<String> generateTree(String catalog, String schema, Connection connection) {
        List<String> tableList = fetchTable(catalog, schema, connection);
        return tableList.stream().map(tableName -> {
            Map<String, Object> tableInfo = fetchTableInfo(catalog, schema, tableName, connection);
            return tableInfo.get("name") == null ? null : tableInfo.get("name").toString();
        }).collect(Collectors.toList());
    }


    @Override
    public String retrieveMax(DatalinkXJobDetail.Reader reader, String field) throws Exception {
        String fieldName = reader.getTransferSetting().getIncreaseField();
        String catalog = reader.getCatalog();
        String schema = reader.getSchema();
        String tableName = reader.getTableName();

        String maxSql = String.format("select max(%s) from %s ", wrapColumnName(fieldName), wrapTableName(catalog, schema, tableName));

        log.info(String.format("retrieveMax, sql: %s", maxSql));
        Connection connection = (Connection) this.connect(false);
        try (Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet resultSet = stmt.executeQuery(maxSql);
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            log.error("fetch max error", e);
            throw new Exception("fetch max error");
        } finally {
            this.disConnect(connection);
        }
        return null;
    }

    @Override
    public void truncateData(DatalinkXJobDetail.Writer writer) throws Exception {
        String catalog = writer.getCatalog();
        String schema = writer.getSchema();
        String tableName = writer.getTableName();
        String fullTableName = wrapTableName(catalog, schema, tableName);

        Connection connection = (Connection) this.connect(false);
        try (Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            stmt.execute(String.format("truncate table %s", fullTableName));
        } catch (SQLException e) {
            log.error(String.format("truncate table (%s) failed", fullTableName), e);
        } finally {
            this.disConnect(connection);
        }
    }


    public List<String> fetchTable(String catalog, String schema, Connection connection) {
        List<String> tables = new ArrayList<>();
        try {
            ResultSet resultSet = connection.getMetaData()
                    .getTables(catalog, schema, null, new String[]{"TABLE", "VIEW"});
            while (resultSet.next()) {
                if (!StringUtils.equals(resultSet.getString("TABLE_TYPE"), "SYSTEM TABLE")) {
                    tables.add(resultSet.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            log.error("fetch tables error", e);
        }

        return tables;
    }

    public Map<String, Object> fetchTableInfo(String catalog, String schema, String table, Connection connection) {
        Map<String, Object> result = new HashMap<>();

        String tableEscaped = table.replace(".", "%");

        ResultSet rs;
        try {
            rs = connection.getMetaData().getTables(catalog, schema, tableEscaped, null);
            while (rs.next()) {
                if (StringUtils.equals(rs.getString("TABLE_NAME"), table)) {
                    result.put("name", rs.getString("TABLE_NAME"));
                    result.put("remark", rs.getString("REMARKS"));
                    result.put("type", rs.getString("TABLE_TYPE"));
                }
            }
        } catch (SQLException e) {
            log.error("fetch table info error", e);
            throw new RuntimeException(e);
        }

        return result;
    }

    public List<Map<String, Object>> fetchColumn(String catalog, String schema, String table, Connection connection) {
        List<Map<String, Object>> result = new ArrayList<>();

        String tableEscaped = table.replace(".", "%");

        try {
            ResultSet rs = connection.getMetaData().getTables(catalog, schema, tableEscaped, null);
            boolean isExists = false;
            while (rs.next()) {
                if (StringUtils.equals(rs.getString("TABLE_NAME"), table)) {
                    isExists = true;
                    break;
                }
            }

            if (!isExists) {
                return new ArrayList<>();
            }

            ResultSet columns = connection.getMetaData().getColumns(catalog, schema, tableEscaped, null);
            while (columns.next()) {
                if (StringUtils.equals(table, columns.getString("TABLE_NAME"))) {
                    HashMap<String, Object> column = new HashMap<>();
                    column.put("name", columns.getString("COLUMN_NAME"));
                    column.put("type", columns.getString("TYPE_NAME"));
                    result.add(column);
                }
            }
        } catch (SQLException e) {
            log.error("fetch column error", e);
            throw new RuntimeException(e);
        }

        return result;
    }


    @Override
    public Object getReaderInfo(DatalinkXJobDetail.Reader reader) throws Exception {

        // 若是增量，查询最大值，作为下次的起始值
        String whereSql = this.genWhere(reader);

        ReaderInfo<P> readerInfo = new ReaderInfo<>();
        String schema = reader.getSchema();
        readerInfo.setName(reader.getType().toLowerCase() + "reader");

        readerInfo.setParameter((P) JdbcReader.builder()
                .username(jdbcSetupInfo.getUid())
                .password(jdbcSetupInfo.getPwd())
                .fetchSize(reader.getTransferSetting().getFetchSize())
                .queryTimeOut(reader.getTransferSetting().getQueryTimeOut())
                .connection(Collections.singletonList(ReaderConnection.builder()
                        .jdbcUrl(Collections.singletonList(jdbcUrl()))
                        .table(Collections.singletonList(reader.getTableName()))
                        .schema(schema)
                        .build()))
                .column(
                        ObjectUtils.isEmpty(reader.getColumns()) ?
                                Collections.singletonList("*") : reader.getColumns())
                .where(whereSql)
                .build());

        return readerInfo;
    }

    public Object getWriterInfo(DatalinkXJobDetail.Writer writer) {
        WriterInfo<Q> jdbcWriterInfo = new WriterInfo<>();


        String schema = writer.getSchema();
        jdbcWriterInfo.setName(writer.getType().toLowerCase() + "writer");
        jdbcWriterInfo.setParameter((Q) JdbcWriter.builder()
                .username(jdbcSetupInfo.getUid())
                .password(jdbcSetupInfo.getPwd())
                .connection(Collections.singletonList(WriterConnection.builder()
                        .schema(schema)
                        .jdbcUrl(jdbcUrl())
                        .table(Collections.singletonList(writer.getTableName()))
                        .build()))
                .column(writer.getColumns())
                .insertSqlMode("copy")
                .writeMode("insert")
                .batchSize(writer.getBatchSize())
                .build());

        return jdbcWriterInfo;
    }

    public String columnQuota() {
        return "`";
    }

    public String valueQuota() {
        return "'";
    }

    public String wrapTableName(String catalog, String schema, String tableName) {
        List<String> fullName = new ArrayList<>();
        if (StringUtils.isNotEmpty(catalog)) {
            fullName.add(String.format("%s%s%s", columnQuota(), catalog, columnQuota()));
        }

        if (StringUtils.isNotEmpty(schema)) {
            fullName.add(String.format("%s%s%s", columnQuota(), schema, columnQuota()));
        }

        if (StringUtils.isNotEmpty(tableName)) {
            fullName.add(String.format("%s%s%s", columnQuota(), tableName, columnQuota()));
        }
        return String.join(".", fullName);
    }

    @Override
    public TransformNode getSourceInfo(DatalinkXJobDetail.Reader reader) {

        return JdbcSource.builder()
                .url(this.jdbcUrl())
                .driver(this.driverClass())
                .user(this.jdbcSetupInfo.getUid())
                .password(this.jdbcSetupInfo.getPwd())
                .query(this.transferSourceSQL(reader))
                .pluginName(PLUGIN_NAME)
                .resultTableName(MetaConstants.CommonConstant.SOURCE_TABLE)
                .build();
    }

    @Override
    public String transferSourceSQL(DatalinkXJobDetail.Reader reader) {
        String sourceSQL = String.format("select %s from %s.%s", reader.getQueryFields(), reader.getSchema(), reader.getTableName());
        try {

            String increaseSQL = this.genWhere(reader);
            if (!ObjectUtils.isEmpty(increaseSQL)) {
                sourceSQL += String.format(" where %s", increaseSQL);
            }
        } catch (Exception e) {

            log.error("gen increase sql error: " + e.getMessage(), e);
            throw new DatalinkXJobException("生成增量条件SQL失败");
        }

        return sourceSQL;
    }

    @Override
    public TransformNode getSinkInfo(DatalinkXJobDetail.Writer writer) {


        return JdbcSink.builder()
                .url(this.jdbcUrl())
                .driver(this.driverClass())
                .user(this.jdbcSetupInfo.getUid())
                .password(this.jdbcSetupInfo.getPwd())
                .pluginName(PLUGIN_NAME)
                .query(this.transferSinkSQL(writer))
                .build();
    }

    @Override
    public String transferSinkSQL(DatalinkXJobDetail.Writer writer) {
        StringBuilder abstractQuery = new StringBuilder();
        for (int i = 0; i < writer.getInsertFields().split(",").length; i++) {
            if (i == 0) {

                abstractQuery.append("?");
            } else {

                abstractQuery.append(",?");
            }
        }


        return String.format("insert into %s.%s(%s) values(%s)", writer.getSchema(), writer.getTableName(), writer.getInsertFields(), abstractQuery);
    }
}
