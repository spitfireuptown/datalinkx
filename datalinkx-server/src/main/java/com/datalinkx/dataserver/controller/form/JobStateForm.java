package com.datalinkx.dataserver.controller.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobStateForm {
    private String jobId;
    private String jobTaskId;
    private Integer jobStatus;
    private Long startTime;
    private Long endTime;
    private String errmsg;
    private String checkpoint;
    private Long readCount = 0L;
    private Long writeCount = 0L;
}
