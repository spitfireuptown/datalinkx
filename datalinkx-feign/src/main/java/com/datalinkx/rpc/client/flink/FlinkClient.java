package com.datalinkx.rpc.client.flink;

import com.datalinkx.rpc.client.flink.request.FlinkJobStopReq;
import com.datalinkx.rpc.client.flink.request.FlinkSavepointReq;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "flinkClient", url = "${client.flink.url}")
@ConditionalOnProperty(prefix = "client.flink", name = "url")
public interface FlinkClient {

    @RequestMapping("/jobs/overview")
    JsonNode jobOverview();

    @RequestMapping("/jobs/{jobId}/exceptions")
    JsonNode jobExceptions(@PathVariable("jobId") String jobId);

    @RequestMapping("/v1/jobs/{jobId}")
    JsonNode jobStatus(@PathVariable("jobId") String jobId);

    @RequestMapping("/v1/jobs/{jobId}/accumulators")
    JsonNode jobAccumulators(@PathVariable("jobId") String jobId);

    @RequestMapping("/jobs/{jobId}/checkpoints")
    JsonNode jobCheckpoint(@PathVariable("jobId") String jobId);

    @RequestMapping("/jobs/{jobId}/savepoints")
    JsonNode jobSavePoints(@PathVariable("jobId") String jobId, @RequestBody FlinkSavepointReq flinkSavepointReq);

    @RequestMapping("/jobs/{jobId}/stop")
    JsonNode jobStop(@PathVariable("jobId") String jobId, @RequestBody FlinkJobStopReq flinkJobStopReq);
}
