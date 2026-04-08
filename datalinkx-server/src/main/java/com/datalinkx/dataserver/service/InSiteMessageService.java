package com.datalinkx.dataserver.service;

import com.datalinkx.dataserver.bean.dto.InSiteMessageDto;
import com.datalinkx.dataserver.bean.domain.InSiteMessageBean;

import java.util.List;

public interface InSiteMessageService {
    void createMessage(InSiteMessageDto messageDto);
    void updateMessage(InSiteMessageDto messageDto);
    void deleteMessage(Long id);
    InSiteMessageDto getMessageById(Long id);
    List<InSiteMessageDto> getMessagesByUserId(String userId);
    List<InSiteMessageDto> getUnreadMessagesByUserId(String userId);
    void markAsRead(Long id);
    void markAllAsRead(String userId);
}
