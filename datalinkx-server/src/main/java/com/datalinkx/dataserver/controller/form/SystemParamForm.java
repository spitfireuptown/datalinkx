package com.datalinkx.dataserver.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

public class SystemParamForm {

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class SystemParamCreateForm {
		@JsonProperty("id")
		private Long id;
		@JsonProperty("param_key")
		private String paramKey;
		@JsonProperty("param_value")
		private String paramValue;
		@JsonProperty("param_desc")
		private String paramDesc;
		@JsonProperty("param_scope")
		private String paramScope;
		@JsonProperty("enable")
		private Boolean enable;
	}

	@Data
	public static class SystemParamPageForm {
		@JsonProperty("param_key")
		private String paramKey;
		@JsonProperty("param_scope")
		private String paramScope;
		@ApiModelProperty(value = "当前页")
		@JsonProperty("page_no")
		private Integer pageNo = 1;
		@ApiModelProperty(value = "展示数量")
		@JsonProperty("page_size")
		private Integer pageSize = 10;
	}
}