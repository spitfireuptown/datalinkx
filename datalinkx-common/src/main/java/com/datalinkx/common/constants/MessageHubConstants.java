package com.datalinkx.common.constants;

public class MessageHubConstants {

    public static final String REDIS_STREAM_TYPE = "REDIS_STREAM";
    public static final String REDIS_PUBSUB_TYPE = "REDIS_PUBSUB";
    public static final String REDIS_QUEUE_TYPE = "REDIS_QUEUE";

    public static final String GLOBAL_PREFIX = "MESSAGEHUB";
    public static final String GLOBAL_COMMON_GROUP = "datalinkx";

    public static final String JOB_PROGRESS_TOPIC = "JOB_PROGRESS";
    public static final String JOB_ALARM_PUSH = "JOB_ALARM_PUSH";


    /**
     * 生产者、消费者保护脏的topic
     */
    public static String getExternalTopicName(String type, String topic) {
        return String.format("%s:%s:%s", GLOBAL_PREFIX, type, topic);
    }
}
