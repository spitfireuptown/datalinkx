package com.datalinkx.dataserver.bean.domain;

import io.swagger.annotations.ApiModel;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "系统参数")
@Data
@FieldNameConstants
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "SYSTEM_PARAM")
public class SystemParamBean extends BaseDomainBean implements Serializable {
	private static final long serialVersionUID = 1L;
	@Column(name = "param_key", length = 128, nullable = false)
	private String paramKey;
	@Column(name = "param_value", length = 2000)
	private String paramValue;
	@Column(name = "param_desc", length = 512)
	private String paramDesc;
	@Column(name = "param_scope", length = 64)
	private String paramScope;
	@Column(name = "enable")
	private Boolean enable;
}