package com.datalinkx.driver.dsdriver;


import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.driver.dsdriver.base.meta.DbTableField;
import com.datalinkx.driver.dsdriver.base.reader.AbstractReader;
import com.datalinkx.driver.dsdriver.transformdriver.TransformNode;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public interface IDsReader extends IDsDriver {
    // ============= Flinkx 引擎
    String retrieveMax(DatalinkXJobDetail.Reader reader, String field) throws Exception;
    Object getReaderInfo(DatalinkXJobDetail.Reader reader) throws Exception;
    List<String> treeTable(String catalog, String schema) throws Exception;
    List<DbTableField> getFields(String catalog, String schema, String tableName) throws Exception;

    default Boolean judgeIncrementalField(String catalog, String schema,
                                          String tableName, String field) throws Exception {
        List<DbTableField> incrementalFields = this.getFields(catalog, schema, tableName)
                .stream().filter(tableField -> tableField.getName().equals(field))
                .collect(Collectors.toList());

        if (ObjectUtils.isEmpty(incrementalFields)) {
            throw new Exception("增量字段不存在");
        }
        return this.incrementalFields().contains(incrementalFields.get(0).getType().toLowerCase());
    }

    default Set<String> incrementalFields() {
        return new HashSet<String>() {{
            add("datetime");
            add("date");
            add("timestamp");
            add("time");
            add("int");
            add("double");
            add("long");
            add("bigint");
            add("bigint unsigned");
        }};
    }

    default String genWhere(DatalinkXJobDetail.Reader reader) throws Exception {

        if (reader.getTransferSetting().getIncreaseField() != null) {
            // 1、获取增量字段
            String field = reader.getTransferSetting().getIncreaseField();
            // 2、获取增量条件
            String fieldType = reader.getTransferSetting().getIncreaseFieldType();

            IDsReader readDsDriver = DsDriverFactory.getDsReader(reader.getConnectId());
            // 3、如果不是首次增量同步，取上一次同步字段最大值
            if (!StringUtils.isEmpty(reader.getMaxValue())) {
                String maxValue = reader.getMaxValue();

                // 3.1、更新下一次的增量条件
                String nextMaxValue = readDsDriver.retrieveMax(reader, field);
                if (!ObjectUtils.isEmpty(nextMaxValue)) {
                    reader.setMaxValue(nextMaxValue);
                }

                return String.format(" %s > %s ", wrapColumnName(field), wrapValue(fieldType, maxValue));
            }
            // 4、如果是首次增量同步，上一次同步字段最大值为空，保存到下次
            String nextMaxValue = readDsDriver.retrieveMax(reader, field);
            if (!ObjectUtils.isEmpty(nextMaxValue)) {
                reader.setMaxValue(nextMaxValue);
            }
        }
        return " (1=1) ";
    }

    // ============= Seatunnel引擎
    // 构造seatunnel引擎读信息
    default AbstractReader getSourceInfo(DatalinkXJobDetail.Reader reader) throws Exception {
        return null;
    }

    // 构造seatunnel引擎source中sql
    default String transferSourceSQL(DatalinkXJobDetail.Reader reader) {
        return "";
    }
}
