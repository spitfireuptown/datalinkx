package com.datalinkx.rpc.client.seatunnel.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * @author: uptown
 * @date: 2024/11/22 23:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobOverviewResp {
    private String jobId;
    private String jobName;
    private String jobStatus;
    private String errorMsg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp finishTime;
    private Metrics metrics;


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metrics {
        @JsonProperty("SourceReceivedCount")
        private Object sourceReceivedCount;
        @JsonProperty("SourceReceivedQPS")
        private Object sourceReceivedQPS;
        @JsonProperty("SourceReceivedBytes")
        private Object sourceReceivedBytes;
        @JsonProperty("SourceReceivedBytesPerSeconds")
        private Object sourceReceivedBytesPerSeconds;
        @JsonProperty("SinkWriteCount")
        private Object sinkWriteCount;
        @JsonProperty("SinkWriteQPS")
        private Object sinkWriteQPS;
        @JsonProperty("SinkWriteBytes")
        private Object sinkWriteBytes;
        @JsonProperty("SinkWriteBytesPerSeconds")
        private Object sinkWriteBytesPerSeconds;
        @JsonProperty("TableSourceReceivedCount")
        private Object tableSourceReceivedCount;
        @JsonProperty("TableSourceReceivedQPS")
        private Object tableSourceReceivedQPS;
        @JsonProperty("TableSourceReceivedBytes")
        private Object tableSourceReceivedBytes;
        @JsonProperty("TableSourceReceivedBytesPerSeconds")
        private Object tableSourceReceivedBytesPerSeconds;
        @JsonProperty("TableSinkWriteCount")
        private Object tableSinkWriteCount;
        @JsonProperty("TableSinkWriteQPS")
        private Object tableSinkWriteQPS;
        @JsonProperty("TableSinkWriteBytes")
        private Object tableSinkWriteBytes;
        @JsonProperty("TableSinkWriteBytesPerSeconds")
        private Object tableSinkWriteBytesPerSeconds;
        @JsonProperty("SinkReceiveQPS")
        private Object sinkReceiveQPS;
        @JsonProperty("SinkReceiveBytes")
        private Object sinkReceiveBytes;
    }
}
