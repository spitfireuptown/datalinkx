package com.datalinkx.httpdriver;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.common.utils.TelnetUtil;
import com.datalinkx.compute.connector.http.HttpSource;
import com.datalinkx.compute.connector.jdbc.TransformNode;
import com.datalinkx.driver.dsdriver.IDsReader;
import com.datalinkx.driver.dsdriver.base.AbstractDriver;
import com.datalinkx.driver.dsdriver.base.meta.DbTableField;
import com.datalinkx.driver.dsdriver.base.writer.AbstractWriter;
import com.datalinkx.driver.dsdriver.setupinfo.HttpSetupInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: uptown
 * @date: 2024/12/17 22:38
 */
public class HttpDriver extends AbstractDriver<HttpSetupInfo, HttpReader, AbstractWriter> implements IDsReader {

    private final String connectId;
    private final HttpSetupInfo httpSetupInfo;
    protected String PLUGIN_NAME = "Http";

    public HttpDriver(String connectId) {
        this.connectId = connectId;
        this.httpSetupInfo = JsonUtils.toObject(ConnectIdUtils.decodeConnectId(connectId) , HttpSetupInfo.class);
    }

    @Override
    public Object connect(boolean check) throws Exception {
        TelnetUtil.telnet(this.httpSetupInfo.getHost(), this.httpSetupInfo.getPort());
        return this.httpSetupInfo;
    }



    @Override
    public String retrieveMax(DatalinkXJobDetail.Reader reader, String field) throws Exception {
        return null;
    }

    @Override
    public Object getReaderInfo(DatalinkXJobDetail.Reader reader) throws Exception {
        return null;
    }

    @Override
    public List<String> treeTable(String catalog, String schema) throws Exception {
        return Collections.singletonList("json_path解析内置表");
    }

    @Override
    public List<DbTableField> getFields(String catalog, String schema, String tableName) throws Exception {
        String revData = this.httpSetupInfo.getRevData();
        JsonNode responseJsonNode = JsonUtils.toJsonNode(revData);
        List<DbTableField> result = new ArrayList<>();

        // 如果json_path解析结果是数组，取数组的第一个元素里的所有key作为HTTP数据源下对应的表
        if (responseJsonNode.isArray()) {
            JsonNode firstElement = responseJsonNode.get(0);
            Iterator<String> fieldNames = firstElement.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                // 接口返回默认数据类型为string
                result.add(DbTableField.builder().name(key).type("string").build());
            }
        } else {
            Iterator<String> fieldNames = responseJsonNode.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                result.add(DbTableField.builder().name(key).type("string").build());
            }
        }
        return result;
    }

    @Override
    public TransformNode getSourceInfo(DatalinkXJobDetail.Reader reader) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        Map<String, Object> headerMap = new HashMap<>();
        Map<String, Object> bodyMap = new HashMap<>();

        this.httpSetupInfo.getParam().forEach(item -> paramMap.put(item.getKey(), item.getValue()));
        this.httpSetupInfo.getHeader().forEach(item -> headerMap.put(item.getKey(), item.getValue()));
        this.httpSetupInfo.getBody().forEach(item -> bodyMap.put(item.getKey(), item.getValue()));
        String baseUrl = this.httpSetupInfo.getUrl();

        if ("GET".equalsIgnoreCase(this.httpSetupInfo.getMethod()) && !ObjectUtils.isEmpty(paramMap)) {
            List<String> paramList = new ArrayList<>();
            for (Map.Entry<String, Object> paramItem : paramMap.entrySet()) {
                paramList.add(paramItem.getKey() + "=" + paramItem.getValue());
            }
            String paramUrl = String.join("&", paramList);
            baseUrl = String.format("%s?%s", baseUrl, paramUrl);
        }

        List<String> responseFields = this.getFields("This", "is", "easter egg")
                .stream().map(DbTableField::getName)
                .collect(Collectors.toList());

        // 接口字段配置
        HttpSource.Schema schema = new HttpSource.Schema();
        // 为什么用LinkedHashMap？ 因为要保证写入顺序与页面上配置的字段映射顺序一致
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        for (String column : reader.getColumns()) {
            if (!responseFields.contains(column)) {
                // 如果不是接口返回的字段，跳过处理
                continue;
            }
            fields.put(column, "string");
        }
        schema.setFields(fields);

        String revData = this.httpSetupInfo.getRevData();
        JsonNode responseJsonNode = JsonUtils.toJsonNode(revData);
        Map<String, String> jsonField = new HashMap<>();

        for (String column : reader.getColumns()) {
            if (!responseFields.contains(column)) {
                // 如果不是接口返回的字段，跳过处理
                continue;
            }

            String fieldJsonPath;
            if (responseJsonNode.isArray()) {
                fieldJsonPath = this.httpSetupInfo.getJsonPath() + "[*]." + column;
            } else {
                fieldJsonPath = this.httpSetupInfo.getJsonPath() + "." + column;
            }
            jsonField.put(column, fieldJsonPath);
        }


        return HttpSource.builder()
                .url(baseUrl)
                .method(this.httpSetupInfo.getMethod())
//                .contentField(this.httpSetupInfo.getJsonPath())
                .jsonField(jsonField)
                .params(bodyMap)
                .headers(headerMap)
                .body(this.httpSetupInfo.getRaw())
                .schema(schema)
                .pluginName(PLUGIN_NAME)
                .resultTableName(MetaConstants.CommonConstant.SOURCE_TABLE)
                .build();
    }
}
