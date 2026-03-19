package com.datalinkx.esdriver;

import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.driver.dsdriver.IDsReader;
import com.datalinkx.driver.dsdriver.IDsWriter;
import com.datalinkx.driver.dsdriver.base.AbstractDriver;
import com.datalinkx.driver.dsdriver.base.meta.DbTableField;
import com.datalinkx.driver.dsdriver.base.reader.ReaderInfo;
import com.datalinkx.driver.dsdriver.base.writer.WriterInfo;
import com.datalinkx.driver.dsdriver.setupinfo.EsSetupInfo;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


public class EsDriver extends AbstractDriver<EsSetupInfo, EsReader, EsWriter> implements IDsReader, IDsWriter {
    private static final long ES_TIMEOUT = 3000L;
    private static final long DEFAULT_FETCH_SIZE = 10000L;

    private final EsSetupInfo esSetupInfo;
    private final EsService esService;

    public EsDriver(String connectId) {
        this.esSetupInfo = JsonUtils.toObject(ConnectIdUtils.decodeConnectId(connectId) , EsSetupInfo.class);
        this.esSetupInfo.setPwd(rebuildPassword(esSetupInfo.getPwd()));
        this.esService = new OpenEsService(esSetupInfo, this);
    }

    @Override
    public Object connect(boolean check) throws Exception {
        return esService.getClient();
    }


    private Map<String, Object> getBoolQuery(DatalinkXJobDetail.Reader reader) throws Exception {

        List<Map<String, Object>> mustList = new ArrayList<>();
        if (reader.getTransferSetting().getIncreaseField() != null) {
            String field = reader.getTransferSetting().getIncreaseField();
            String fieldType = reader.getTransferSetting().getIncreaseFieldType();


            if ("increment".equalsIgnoreCase(reader.getTransferSetting().getType())) {
                String startValue = reader.getTransferSetting().getStart();
                if (startValue != null) {
                    mustList.add(esService.buildRange(field, startValue, ">", "must", fieldType));
                }

                String endValue = reader.getTransferSetting().getEnd();
                if (endValue != null) {
                    mustList.add(esService.buildRange(field, endValue, "<", "must", fieldType));
                }
            }
        }


        Map<String, Object> queryMap = new HashMap<>();
        if (!mustList.isEmpty()) {
            queryMap.put("must", mustList);
        }

        Map<String, Object> boolMap = new HashMap<>();
        boolMap.put("bool", queryMap);

        return boolMap;
    }

    @Override
    public String retrieveMax(DatalinkXJobDetail.Reader reader, String maxField) throws Exception {
        Map<String, Object> boolMap = getBoolQuery(reader);
        Map<String, Object> queryMap = new HashMap<String, Object>() {
            {
                put("query", boolMap);
                put("sort", Lists.newArrayList(new HashMap<String, Object>() {
                    {
                        put(maxField, "desc");
                    }
                }));
                put("size", 1);
            }
        };
        return esService.retrieveMax(reader.getTableName(), JsonUtils.toJson(queryMap), maxField);
    }


    @Override
    public Object getReaderInfo(DatalinkXJobDetail.Reader reader) throws Exception {
        Map<String, Object> boolMap = getBoolQuery(reader);
        Map<String, Object> mustMap = (Map<String, Object>) boolMap.get("bool");
        if (reader.getTransferSetting().getIncreaseField() != null
                && StringUtils.isEmpty(reader.getMaxValue())) {
            String maxField = reader.getTransferSetting().getIncreaseField();
            String maxValue = retrieveMax(reader, maxField);
            if (null != maxValue) {
                reader.setMaxValue(maxValue);
                String fieldType = reader.getTransferSetting().getIncreaseFieldType();
                if (mustMap.get("must") != null) {
                    ((List<Map<String, Object>>) mustMap.get("must"))
                            .add(esService.buildRange(maxField, maxValue, "<=", "must", fieldType));
                } else {
                    List<Map<String, Object>> mustList = new ArrayList<>();
                    mustList.add(esService.buildRange(maxField, maxValue, "<=", "must", fieldType));
                    mustMap.put("must", mustList);
                }
            }
        }

        List<String> types = new ArrayList<>();
        types.addAll(esService.getIndexType(reader.getTableName()));

        ReaderInfo<EsReader> readerInfo = new ReaderInfo<>();
        readerInfo.setName("esreader");


        readerInfo.setParameter(EsReader.builder()
                .address(esSetupInfo.getAddress())
                .username(esSetupInfo.getUid())
                .password(esSetupInfo.getPwd())
                .batchSize(reader.getTransferSetting().getFetchSize() == null ? DEFAULT_FETCH_SIZE
                        : reader.getTransferSetting().getFetchSize().longValue())
                .index(reader.getTableName())
                .type(types.toArray(new String[0]))
                .query(JsonUtils.toJsonNode(JsonUtils.toJson(boolMap)))
                .timeout(ES_TIMEOUT).column(reader.getColumns())
                .build());

        return readerInfo;
    }


    @Override
    public List<String> treeTable(String catalog, String schema) throws Exception {
        List<String> dbTreeTables = new ArrayList<>();
        if ("".equals(catalog)) {
            List<String> indexes = esService.getIndexes();
            dbTreeTables.addAll(indexes);
        }
        return dbTreeTables;
    }

    @Override
    public List<DbTableField> getFields(String catalog, String schema, String tableName) throws Exception {
        return esService.getFields(tableName);
    }


    @Override
    public void truncateData(DatalinkXJobDetail.Writer writer) throws Exception {
        esService.truncateData(writer.getTableName());
    }

    @Override
    public Object getWriterInfo(DatalinkXJobDetail.Writer writer) throws Exception {
        String tableName = writer.getTableName();
        WriterInfo<EsWriter> writerInfo = new WriterInfo<>();
        // es 7.x后续版本后取消了索引类型
        List<String> indexTypeList = this.esService.getIndexType(tableName);
        String indexType = ObjectUtils.isEmpty(indexTypeList) ? "_doc" : indexTypeList.get(0);

        writerInfo.setName("eswriter");
        writerInfo.setParameter(EsWriter.builder()
                .address(esSetupInfo.getAddress())
                .username(esSetupInfo.getUid())
                .password(esSetupInfo.getPwd())
                .timeout(ES_TIMEOUT)
                .bulkAction(DEFAULT_FETCH_SIZE)
                .index(tableName)
                .type(indexType)
                .column(writer.getColumns())
                .build());
        return writerInfo;
    }


    @Override
    public Set<String> incrementalFields() {
        return new HashSet<String>() {{
            add("number");
            add("date");
        }};
    }
}
