package com.datalinkx.driver.dsdriver.base.meta;

import com.datalinkx.common.result.DatalinkXJobDetail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

/**
 * 执行引擎任务类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlinkActionMeta extends EngineActionMeta {
    // datalinkx-server业务来源库信息
    public DatalinkXJobDetail.Reader reader;
    // datalinkx-server业务目标库信息
    public DatalinkXJobDetail.Writer writer;
    // 是否覆盖 0 - 否 1 - 是
    public Integer cover;
    // 数据总读行数
    public long readRecords = 0L;
    // 数据总写行数
    public long writeRecords = 0L;
}
