package com.datalinkx.dataserver.bean.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 动态编译相关 VO
 */
public class DynamicCodeVo {

    /**
     * 校验结果
     */
    @Data
    @Builder
    public static class ValidateResult {
        private boolean valid;
        private String message;
        private List<OutputField> outputFields;
    }

    /**
     * 输出字段
     */
    @Data
    @Builder
    public static class OutputField {
        private String name;
        private String type;
    }
}
