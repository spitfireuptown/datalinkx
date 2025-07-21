package com.datalinkx.common.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.Map;



@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
@Builder
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatalinkXJobDetail {
    @JsonProperty("job_id")
    String jobId;
    Integer type;
    @JsonProperty("lock_id")
    String lockId;
    @JsonProperty("sync_unit")
    @JsonIgnoreProperties(ignoreUnknown = true)
    SyncUnit syncUnit;
    Integer cover;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SyncUnit {
        Reader reader;
        Writer writer;
        Compute compute;
        String checkpointPath;
        // 公共配置
        Map<String, Object> commonSettings;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Compute {
        // 算子列表
        private List<Transform> transforms;
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Transform {
            // 算子类型
            private String type;
            // 算子内容
            private String meta;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Reader {
        String catalog;
        String schema;
        String tableName;
        List<String> columns;
        TransferSetting transferSetting;
        String connectId;
        String type;
        String maxValue;
        String queryFields;
        Object readerGraph;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Writer {
        String catalog;
        String schema;
        String tableName;
        List<String> columns;
        String connectId;
        Integer batchSize;
        String type;
        String insertFields;
        String writeMode;
        String updateKey;
        Object writerGraph;
    }



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TransferSetting {
        String type; // [overwrite全量更新，increment增量更新]
        Integer fetchSize = 10000; // 每次获取的条数
        Integer queryTimeOut = 10000; // flinkx查询执行超时时间
        String increaseField;
        String increaseFieldType;
        String start;
        String end; // 暂时没有end
    }

}
