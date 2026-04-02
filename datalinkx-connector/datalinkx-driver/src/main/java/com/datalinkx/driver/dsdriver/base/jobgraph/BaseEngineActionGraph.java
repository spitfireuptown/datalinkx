package com.datalinkx.driver.dsdriver.base.jobgraph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseEngineActionGraph {
    // 流转任务id
    public String jobId;
    // 底层引擎侧任务id
    public String taskId;
}
