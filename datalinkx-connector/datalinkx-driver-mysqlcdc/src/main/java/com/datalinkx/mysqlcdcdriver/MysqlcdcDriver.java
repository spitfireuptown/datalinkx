package com.datalinkx.mysqlcdcdriver;

import com.datalinkx.common.exception.DatalinkXJobException;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.TelnetUtil;
import com.datalinkx.driver.dsdriver.IStreamDriver;
import com.datalinkx.driver.dsdriver.base.AbstractDriver;
import com.datalinkx.driver.dsdriver.base.reader.ReaderInfo;
import com.datalinkx.driver.dsdriver.base.writer.AbstractWriter;
import com.datalinkx.driver.dsdriver.setupinfo.MysqlcdcSetupInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class MysqlcdcDriver extends AbstractDriver<MysqlcdcSetupInfo, MysqlcdcReader, AbstractWriter> implements IStreamDriver  {

    private final MysqlcdcSetupInfo mysqlCDCSetupInfo;
    private final String connectId;

    private static final String MYSQL_DATABASE_JDBC_PATTERN = "jdbc:mysql://%s:%s/%s?useCursorFetch=%s"
            + "&useUnicode=%s&zeroDateTimeBehavior=%s&characterEncoding=%s&useInformationSchema=%s&serverTimezone=%s&socketTimeout=%s"
            + "&useSSL=false";


    public MysqlcdcDriver(String connectId) {
        this.mysqlCDCSetupInfo = JsonUtils.toObject(ConnectIdUtils.decodeConnectId(connectId) , MysqlcdcSetupInfo.class);
        this.mysqlCDCSetupInfo.setPwd(rebuildPassword(this.mysqlCDCSetupInfo.getPwd()));
        this.connectId = connectId;
    }

    protected String jdbcUrl() {
        return String.format(
                MYSQL_DATABASE_JDBC_PATTERN,
                this.mysqlCDCSetupInfo.getServer(),
                this.mysqlCDCSetupInfo.getPort(),
                this.mysqlCDCSetupInfo.getDatabase(),
                this.mysqlCDCSetupInfo.getUseCursorFetch(),
                this.mysqlCDCSetupInfo.getUseUnicode(),
                this.mysqlCDCSetupInfo.getZeroDateTimeBehavior(),
                this.mysqlCDCSetupInfo.getCharacterEncoding(),
                this.mysqlCDCSetupInfo.getUseInformationSchema(),
                this.mysqlCDCSetupInfo.getServerTimezone(),
                this.mysqlCDCSetupInfo.getSocketTimeout()
        );
    }

    @Override
    public Object getReaderInfo(DatalinkXJobDetail.Reader reader) {
        ReaderInfo<MysqlcdcReader> readerInfo = new ReaderInfo<>();
        readerInfo.setName("binlogreader");

        readerInfo.setParameter(MysqlcdcReader.builder()
                        .table(Collections.singletonList(reader.getTableName()))
                        .username(mysqlCDCSetupInfo.getUid())
                        .password(mysqlCDCSetupInfo.getPwd())
                        .database(mysqlCDCSetupInfo.getDatabase())
                        .port(mysqlCDCSetupInfo.getPort())
                        .cat(mysqlCDCSetupInfo.getCat())
                        .host(mysqlCDCSetupInfo.getServer())
                        .jdbcUrl(this.jdbcUrl())
                        .pavingData(mysqlCDCSetupInfo.getPavingData())
                .build());

        return readerInfo;
    }

    @Override
    public Object getWriterInfo(DatalinkXJobDetail.Writer writer) {
        throw new DatalinkXJobException("not support");
    }

    @Override
    public Object connect(boolean check) throws Exception {
        TelnetUtil.telnet(this.mysqlCDCSetupInfo.getServer(), this.mysqlCDCSetupInfo.getPort());
        return this.mysqlCDCSetupInfo;
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql, boolean onlyColumns) throws Exception {
        return Collections.emptyList();
    }
}
