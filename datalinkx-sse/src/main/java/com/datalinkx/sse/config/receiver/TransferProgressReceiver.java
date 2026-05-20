package com.datalinkx.sse.config.receiver;

import com.datalinkx.common.constants.MessageHubConstants;
import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.messagehub.config.annotation.MessageHub;
import com.datalinkx.sse.config.SseEmitterServer;
import org.springframework.stereotype.Service;

/**
 * @author: 任务读写进度实时推送, 负载均衡3个消费者
 * @date: 2023/11/13 21:48
 */
@Service
public class TransferProgressReceiver {

    @MessageHub(
            topic = MessageHubConstants.JOB_PROGRESS_TOPIC,
            group = MessageHubConstants.GLOBAL_COMMON_GROUP,
            type = MessageHubConstants.REDIS_STREAM_TYPE)
    public void pushJobStatusLoadBalance(String jobStatusMsg) {
        SseEmitterServer.sendMessage(MetaConstants.JobStatus.SSE_JOB_STATUS, jobStatusMsg);
    }

    @MessageHub(
            topic = MessageHubConstants.JOB_PROGRESS_TOPIC,
            group = MessageHubConstants.GLOBAL_COMMON_GROUP,
            type = MessageHubConstants.REDIS_STREAM_TYPE)
    public void pushJobStatusLoadBalance2(String jobStatusMsg) {
        SseEmitterServer.sendMessage(MetaConstants.JobStatus.SSE_JOB_STATUS, jobStatusMsg);
    }

    @MessageHub(
            topic = MessageHubConstants.JOB_PROGRESS_TOPIC,
            group = MessageHubConstants.GLOBAL_COMMON_GROUP,
            type = MessageHubConstants.REDIS_STREAM_TYPE)
    public void pushJobStatusLoadBalance3(String jobStatusMsg) {
        SseEmitterServer.sendMessage(MetaConstants.JobStatus.SSE_JOB_STATUS, jobStatusMsg);
    }

    @MessageHub(
        topic = MessageHubConstants.COMPUTE_JOB_PROGRESS_TOPIC,
        group = MessageHubConstants.GLOBAL_COMMON_GROUP,
        type = MessageHubConstants.REDIS_STREAM_TYPE)
    public void pushComputeJobStatusLoadBalance(String jobStatusMsg) {
        SseEmitterServer.sendMessage(MetaConstants.JobStatus.SSE_COMPUTE_JOB_STATUS, jobStatusMsg);
    }
}
