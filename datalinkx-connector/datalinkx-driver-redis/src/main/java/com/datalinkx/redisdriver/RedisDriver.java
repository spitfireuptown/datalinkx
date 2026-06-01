package com.datalinkx.redisdriver;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.driver.dsdriver.IDsWriter;
import com.datalinkx.driver.dsdriver.base.AbstractDriver;
import com.datalinkx.driver.dsdriver.base.reader.AbstractReader;
import com.datalinkx.driver.dsdriver.base.writer.WriterInfo;
import com.datalinkx.driver.dsdriver.setupinfo.RedisSetupInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RedisDriver extends AbstractDriver<RedisSetupInfo, AbstractReader, RedisWriter> implements IDsWriter {

    private final String connectId;
    private final RedisSetupInfo redisSetupInfo;
    private final RedisService redisService;

    public RedisDriver(String connectId) {
        this.connectId = connectId;
        this.redisSetupInfo = JsonUtils.toObject(ConnectIdUtils.decodeConnectId(connectId) , RedisSetupInfo.class);
        this.redisSetupInfo.setPwd(rebuildPassword(redisSetupInfo.getPwd()));
        this.redisService = new RedisService(this.redisSetupInfo);
    }

    @Override
    public Object connect(boolean check) throws Exception {
        return this.redisService.getClient();
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql, boolean onlyColumns) throws Exception {
        return Collections.emptyList();
    }


    @Override
    public String retrieveWriterMax(DatalinkXJobDetail.Reader reader, DatalinkXJobDetail.Writer writer) throws Exception {
        return "";
    }

    @Override
    public void truncateData(DatalinkXJobDetail.Writer writer) throws Exception {

    }

    @Override
    public Object getWriterInfo(DatalinkXJobDetail.Writer writer) throws Exception {
        String tableName = writer.getTableName();
        String[] typeKeyArray = tableName.split(MetaConstants.DsType.REDIS_SPIT_STR);
        String typeArray = typeKeyArray[0];
        String key = typeKeyArray[1];

        String[] typeModeArray = typeArray.split("-");
        String type = typeModeArray[0];
        String mode;
        if (typeModeArray.length > 1) {
            mode = typeModeArray[1];
        } else {
            mode = "set";
        }

        WriterInfo<RedisWriter> writerInfo = new WriterInfo<>();
        writerInfo.setName("rediswriter");
        writerInfo.setParameter(RedisWriter.builder()
                        .customKey(key)
                        .password(redisSetupInfo.getPwd())
                        .hostPort(redisSetupInfo.getHost() + ":" + redisSetupInfo.getPort())
                        .mode(mode)
                        .type(type)
                        .database(redisSetupInfo.getDatabase())
                        // 只是为了补充JobGraph信息，无异议
                        .keyIndexes(new Integer[] {0, 1})
                .build());
        return writerInfo;
    }
}
