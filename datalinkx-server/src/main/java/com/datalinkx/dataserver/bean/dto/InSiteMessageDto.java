package com.datalinkx.dataserver.bean.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InSiteMessageDto {
    @Builder.Default
    private String type = "message";
    private Long id;
    private Long messageId;
    private String title;
    private String time;
    private String avatar;
    @Builder.Default
    private Boolean read = false;
    private String userId;
}
