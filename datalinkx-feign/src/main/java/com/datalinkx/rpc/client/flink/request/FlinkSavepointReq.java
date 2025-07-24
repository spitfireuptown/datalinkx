package com.datalinkx.rpc.client.flink.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FlinkSavepointReq {
    @JsonProperty("target-directory")
    private String targetDirectory;
    @JsonProperty("cancel-job")
    private Boolean cancelJob;
}
