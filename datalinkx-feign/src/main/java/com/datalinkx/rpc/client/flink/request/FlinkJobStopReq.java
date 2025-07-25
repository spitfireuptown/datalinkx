package com.datalinkx.rpc.client.flink.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FlinkJobStopReq {
    private String targetDirectory;
    private Boolean drain;
}
