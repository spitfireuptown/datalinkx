package com.datalinkx.dataserver.receiver;

import com.datalinkx.common.constants.MessageHubConstants;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.dataserver.bean.domain.InSiteMessageBean;
import com.datalinkx.dataserver.bean.dto.InSiteMessageDto;
import com.datalinkx.dataserver.repository.InSiteMessageRepository;
import com.datalinkx.messagehub.config.annotation.MessageHub;
import com.datalinkx.sse.config.SseEmitterServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class InSiteMessageReceiver {

    @Autowired
    InSiteMessageRepository inSiteMessageRepository;

    @MessageHub(
            topic = MessageHubConstants.IN_SITE_MESSAGE_PUSH,
            group = MessageHubConstants.GLOBAL_COMMON_GROUP,
            type = MessageHubConstants.REDIS_STREAM_TYPE
    )
    public void inSiteMessageAccept(String message) {
        InSiteMessageDto inSiteMessageDto = JsonUtils.toObject(message, InSiteMessageDto.class);
        SseEmitterServer.sendMessage(inSiteMessageDto.getUserId(), JsonUtils.toJson(inSiteMessageDto));
        InSiteMessageBean inSiteMessageBean = new InSiteMessageBean();
        BeanUtils.copyProperties(inSiteMessageDto, inSiteMessageBean);
        inSiteMessageBean.setRead(false);
        inSiteMessageBean.setUserId(inSiteMessageDto.getUserId());
        inSiteMessageRepository.save(inSiteMessageBean);
    }
}
