package com.datalinkx.common.constants;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MetaConstants {
    private MetaConstants() {
    }
    /**
     * 数据源类型管理
     */
    public static class DsType {
        // 命名规则与包名一样
        public static final String DS_MYSQL = "mysql";
        public static final String DS_ELASTICSEARCH = "es";
        public static final String DS_ORACLE = "oracle";
        public static final String DS_REDIS = "redis";
        public static final String DS_HTTP = "http";
        public static final String DS_KAFKA = "kafka";
        public static final String DS_MYSQLCDC = "mysqlcdc";
        public static final String DS_CUSTOM = "custom";


        public static final String REDIS_SPIT_STR = "!-!-!";


        public static final List<String> STREAM_DB_LIST = new ArrayList<String>() {{
            add("kafka");
        }};
    }

    public static class JobType {
        public static final Integer JOB_TYPE_COMPUTE = 2;
        public static final Integer JOB_TYPE_STREAM = 1;
        public static final Integer JOB_TYPE_BATCH = 0;
        public static final String JOB_CRON_SCHEDULE_TYPE = "CRON";
        public static final String JOB_NONE_SCHEDULE_TYPE = "FIX_RATE";
        public static final String JOB_RATE_SCHEDULE_CONF = "315360000";
        public static final String STREAM_JOB_HEALTH_CHECK = "stream_health_check";
        public static Map<Integer, String> JOB_TYPE_NAME_MAP = new HashMap<Integer, String>() {{
            put(MetaConstants.JobType.JOB_TYPE_BATCH, "批式任务");
            put(MetaConstants.JobType.JOB_TYPE_STREAM, "流式任务");
            put(MetaConstants.JobType.JOB_TYPE_COMPUTE, "计算任务");
        }};
    }

    public static class JobStatus {
        public static final int JOB_STATUS_CREATE = 0;
        public static final int JOB_STATUS_SYNCING = 1;
        public static final int JOB_STATUS_SUCCESS = 2;
        public static final int JOB_STATUS_ERROR = 3;
        public static final int JOB_STATUS_STOP = 4;
        public static final int JOB_STATUS_QUEUE = 5;
        public static final int JOB_STATUS_STOPPING = 6;

        public static final String SEATUNNEL_JOB_FINISH = "FINISHED";
        public static final String SEATUNNEL_JOB_RUNNING = "RUNNING";
        public static final String SEATUNNEL_JOB_FAILED = "FAILED";
        public static final String SEATUNNEL_SUBMIT_JOB_ERROR_STATUS = "fail";
        public static final String SSE_JOB_STATUS = "jobList";
        public static final String SSE_COPILOT = "datalinkx_copilot";
        public static Map<Integer, String> JOB_STATUS_TO_DB_NAME_MAP = new HashMap<Integer, String>() {{
            put(MetaConstants.JobStatus.JOB_STATUS_ERROR, "任务失败");
            put(MetaConstants.JobStatus.JOB_STATUS_SUCCESS, "任务成功");
            put(MetaConstants.JobStatus.JOB_STATUS_STOP, "任务停止");
            put(MetaConstants.JobStatus.JOB_STATUS_SYNCING, "任务同步中");
            put(MetaConstants.JobStatus.JOB_STATUS_CREATE, "任务新建");
            put(MetaConstants.JobStatus.JOB_STATUS_QUEUE, "任务排队");
        }};
    }

    public static class JobSyncMode {
        public static final String INCREMENT_MODE = "increment";
        public static final String OVERWRITE_MODE = "overwrite";
    }

    public static class CommonConstant {
        public static final String TRACE_ID = "trace_id";
        public static final String SOURCE_TABLE = "source_table";
        public static final String RESULT_TABLE = "result_table";
        public static final String LLM_OUTPUT_TABLE = "llm_output";
        public static final String SQL_OUTPUT_TABLE = "sql_output";
        public static final String TRANSFORM_SQL = "sql";
        public static final String TRANSFORM_LLM = "llm";

        public static final String KEY_RESTORE_COLUMN_INDEX = "restoreColumnIndex";
        public static final String KEY_KAFKA_READ_INDEX = "kafkaReadMode";
        public static final String KEY_CHECKPOINT_INTERVAL = "checkpoint_interval";
        public static final String KEY_RESTORE = "restore";
        public static final String KEY_CHECKPOINT_ENABLE = "isRestore";
        public static final String KEY_CHECKPOINT_INIT_ADDRESS = "checkpointInitAddress";

        public static final Integer KEY_ALARM_RULE_OPEN= 1;
        public static final Integer KEY_ALARM_RULE_CLOSE= 0;
        // 无论成功失败，任务结束后就推送
        public static final Integer ALARM_ALL_STATUS = 0;
        // 失败才推送
        public static final Integer ALARM_FAIL_STATUS = 1;
        // 成功才推送
        public static final Integer ALARM_SUCCESS_STATUS = 2;
        public static final String GLOBAL_ALARM_SUBJECT = "DatalinkX Pro系统告警";
    }

    public static class CopilotConstant {
        public static final String VECTOR_ES_ENGINE = "elasticsearch";
    }
}
