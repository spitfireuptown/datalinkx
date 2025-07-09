package com.datalinkx.dataserver.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


public class JobForm {

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JobCreateForm {
		@ApiModelProperty("任务名称")
		@JsonProperty("job_name")
		private String jobName;
		@ApiModelProperty(value = "数据源id")
		@JsonProperty("from_ds_id")
		private String fromDsId;
		@JsonProperty("to_ds_id")
		private String toDsId;
		@JsonProperty("from_tb_name")
		private String fromTbName;
		@ApiModelProperty(value = "导入表id")
		@JsonProperty("to_tb_name")
		private String toTbName;
		@ApiModelProperty(value = "定时策略")
		@JsonProperty("scheduler_conf")
		private String schedulerConf;
		@ApiModelProperty(value = "字段映射配置")
		@JsonProperty("sync_mode")
		private SyncModeForm syncMode;
		@JsonProperty("field_mappings")
		private List<FieldMappingForm> fieldMappings;
		@ApiModelProperty("是否覆盖数据")
		private Integer cover = 0;
		@ApiModelProperty("任务类型， 2 - 计算, 1 - 流式, 0 - 批式")
		private Integer type = 0;
		@JsonProperty("graph")
		private String graph;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class JobModifyForm extends JobCreateForm {
		@JsonProperty("job_id")
		private String jobId;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class FieldMappingForm {
		@ApiModelProperty(value = "from表字段名称")
		private String sourceField;
		@ApiModelProperty(value = "to表字段名称")
		private String targetField;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class SyncModifyForm {
		@JsonProperty("job_id")
		private String jobId;
		@JsonProperty("increate_value")
		private String increateValue;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SyncModeForm {
		@ApiModelProperty(value = "overwrite全量更新，increment增量更新")
		private String mode = "overwrite";
		@JsonProperty("increate_field")
		private String increateField;
		@JsonProperty("increate_value")
		private String increateValue = "";
		private Boolean checkpoint;
		@JsonProperty("restore_column_index")
		private String restoreColumnIndex;
		@JsonProperty("update_key")
		private String updateKey;
	}

	@Data
	public static class JobPageForm {

		@ApiModelProperty(value = "当前页")
		@JsonProperty("page_no")
		private Integer pageNo = 1;

		@ApiModelProperty(value = "展示数量")
		@JsonProperty("page_size")
		private Integer pageSize = 10;

		@ApiModelProperty(value = "任务类型")
		private Integer type = -1;
	}

	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class JobLogPageForm extends JobPageForm {
		@JsonProperty("job_id")
		private String jobId;
	}

	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class JobRelationPageForm extends JobPageForm {
		@JsonProperty("job_name")
		private String jobName;
	}

	@Data
	public static class JobRelationForm {
		@JsonProperty("job_id")
		private String jobId;
		@JsonProperty("sub_job_id")
		private String subJobId;
		private Integer priority;
	}

	@Data
	public static class HttpTestForm {
		@JsonProperty("api_url")
		private String apiUrl;
		@JsonProperty("content_type")
		private String contentType;
		private List<ItemConfig> header;
		private List<ItemConfig> param;
		private List<ItemConfig> body;
		private String raw;
		private String method;
		@JsonProperty("json_path")
		private String jsonPath;
	}

	@Data
	public static final class ItemConfig {
		private String key;
		private String value;
	}
}
