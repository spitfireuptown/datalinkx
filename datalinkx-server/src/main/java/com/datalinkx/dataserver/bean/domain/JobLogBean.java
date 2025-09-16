package com.datalinkx.dataserver.bean.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@ApiModel(description = "流转任务执行记录")

@Data
@FieldNameConstants
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "JOB_LOG")
public class JobLogBean implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, columnDefinition = "int(11) unsigned")
	private Long id;
	@NotBlank
	@Column(name = "job_id", nullable = false, length = 35, columnDefinition = "varchar(35)")
	private String jobId;
	@ApiModelProperty(value = "0成功 1失败")
	@Column(name = "status", columnDefinition = "tinyint(2) unsigned")
	private Integer status;
	@NotBlank
	@Column(name = "count", nullable = false, columnDefinition = "text")
	private String count;
	@Column(name = "error_msg", columnDefinition = "longtext")
	private String errorMsg;
	@Column(name = "start_time", nullable = false, columnDefinition = "timestamp")
	private Timestamp startTime;
	@Column(name = "end_time", nullable = false, columnDefinition = "timestamp")
	private Timestamp endTime;
	@Column(name = "cost_time", nullable = false, columnDefinition = "int(11) unsigned")
	private Integer costTime;
	@Column(name = "is_del")
	public Integer isDel = 0;
}
