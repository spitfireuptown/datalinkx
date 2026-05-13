package com.datalinkx.dataserver.bean.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DynamicCodeValidateResult {
    private boolean valid;
    private String message;
    private List<OutputField> outputFields;

    @Data
    @Builder
    public static class OutputField {
        private String name;
        private String type;
    }
}
