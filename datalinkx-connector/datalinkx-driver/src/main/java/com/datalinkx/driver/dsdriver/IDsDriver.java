package com.datalinkx.driver.dsdriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface IDsDriver {
    Object connect(boolean check) throws Exception;
    default void disConnect(AutoCloseable conn) {
        if (null != conn) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    default String wrapColumnName(String columnName) {
        return columnQuota() + columnName + columnQuota();
    }
    default String wrapValue(String fieldType, String value) {
        return valueQuota() + value + valueQuota();
    }
    default String valueQuota() {
        return "";
    }
    default String columnQuota() {
        return "";
    }
    List<Map<String, Object>> executeQuery(String sql, boolean onlyColumns) throws Exception;
}
