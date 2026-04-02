package com.datalinkx.driver.dsdriver.base.jobgraph;

import com.datalinkx.driver.dsdriver.base.reader.AbstractReader;
import com.datalinkx.driver.dsdriver.base.writer.AbstractWriter;
import com.datalinkx.driver.dsdriver.transformdriver.TransformNode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeatunnelActionGraph extends FlinkActionGraph {
    private AbstractReader sourceInfo;
    private AbstractWriter sinkInfo;
    // 计算任务的中间节点
    private List<TransformNode> transformInfo;
    private String jobMode;
    private Integer parallelism;
    Integer cover;
}
