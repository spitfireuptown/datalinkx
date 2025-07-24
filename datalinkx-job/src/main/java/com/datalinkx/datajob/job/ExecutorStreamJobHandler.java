package com.datalinkx.datajob.job;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.datalinkx.common.constants.MetaConstants.CommonConstant.KEY_CHECKPOINT_INTERVAL;

@Slf4j
@Service
public class ExecutorStreamJobHandler extends ExecutorJobHandler {

    public String execute(String jobId, String reader, String writer, Map<String, Object> otherSetting) throws Exception {
        return super.execute(jobId, reader, writer, otherSetting);
    }


    @Override
    public String generateFlinkCmd(String jobId, String jobJsonFile, Map<String, Object> otherSetting) {
        String javaHome = System.getenv("JAVA_HOME");
        String os = System.getProperty("os.name").toLowerCase();

        String executeCmd = String.format(
                "%s -cp %s com.dtstack.flinkx.launcher.Launcher -mode standalone -jobid %s" +
                        "  -job %s  -pluginRoot %s -flinkconf %s",
                javaHome + (os.contains("win") ? "\\bin\\java" : "/bin/java"),
                flinkXHomePath + (os.contains("win") ? "lib\\*" : "lib/*"),
                jobId,
                jobJsonFile,
                flinkXHomePath + "syncplugins",
                flinkXHomePath + "flinkconf"
        );

        // 如果开启checkpoint定义定时规则，每到flink.checkpoint.interval写入一批事务
        Boolean checkpointEnable = (Boolean) otherSetting.get(MetaConstants.CommonConstant.KEY_CHECKPOINT_ENABLE);
        if (checkpointEnable) {
            executeCmd += String.format(" -confProp {\\\"flink.checkpoint.interval\\\":%s} ", otherSetting.getOrDefault(KEY_CHECKPOINT_INTERVAL, 6000));
        }

        if (!ObjectUtils.isEmpty(otherSetting.get("savePointPath"))) {
            executeCmd = executeCmd + " -s " + otherSetting.get("savePointPath");
        }

        return executeCmd;
    }

    @SneakyThrows
    @Override
    public String generateJobSetting(String jobSettingPath, Map<String, Object> otherSetting) {
        String jobSetting = super.generateJobSetting("classpath:stream_setting.json", otherSetting);
        Map jobSettingMap = JsonUtils.toObject(jobSetting, Map.class);
        Map<String, Object> restoreMap = (Map<String, Object>) jobSettingMap.get(MetaConstants.CommonConstant.KEY_RESTORE);
        restoreMap.put(MetaConstants.CommonConstant.KEY_RESTORE_COLUMN_INDEX, otherSetting.get(MetaConstants.CommonConstant.KEY_RESTORE_COLUMN_INDEX));
        restoreMap.put(MetaConstants.CommonConstant.KEY_CHECKPOINT_ENABLE, otherSetting.get(MetaConstants.CommonConstant.KEY_CHECKPOINT_ENABLE));
        return JsonUtils.toJson(jobSettingMap);
    }
}
