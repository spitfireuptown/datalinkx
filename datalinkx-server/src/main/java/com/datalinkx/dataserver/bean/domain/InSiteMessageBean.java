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
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "站内信")
@Data
@FieldNameConstants
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "IN_SITE_MESSAGE")
public class InSiteMessageBean extends BaseDomainBean {
    @Column(name = "message_id", nullable = false)
    private Long messageId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "content", nullable = false)
    private String content;
    @Column(name = "type", nullable = false)
    private String type;
    @Column(name = "avatar", nullable = false)
    private String avatar;
    @Column(name = "is_read", nullable = false)
    private Boolean read;
}
