package com.datalinkx.dataserver.service.impl;

import com.datalinkx.dataserver.bean.dto.InSiteMessageDto;
import com.datalinkx.dataserver.bean.domain.InSiteMessageBean;
import com.datalinkx.dataserver.repository.InSiteMessageRepository;
import com.datalinkx.dataserver.service.InSiteMessageService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class InSiteMessageServiceImpl implements InSiteMessageService {

    @Autowired
    private InSiteMessageRepository inSiteMessageRepository;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void createMessage(InSiteMessageDto messageDto) {
        InSiteMessageBean messageBean = new InSiteMessageBean();
        BeanUtils.copyProperties(messageDto, messageBean);
        messageBean.setMessageId(System.currentTimeMillis());
        messageBean.setIsDel(0);
        messageBean.setRead(messageDto.getRead());
        inSiteMessageRepository.save(messageBean);
    }

    @Override
    public void updateMessage(InSiteMessageDto messageDto) {
        InSiteMessageBean messageBean = new InSiteMessageBean();
        BeanUtils.copyProperties(messageDto, messageBean);
        messageBean.setRead(messageDto.getRead());
        inSiteMessageRepository.save(messageBean);
    }

    @Override
    public void deleteMessage(Long id) {
        inSiteMessageRepository.logicDeleteById(id);
    }

    @Override
    public InSiteMessageDto getMessageById(Long id) {
        java.util.Optional<InSiteMessageBean> optional = inSiteMessageRepository.findById(id);
        if (!optional.isPresent ()) {
            return null;
        }
        InSiteMessageBean messageBean = optional.get();
        InSiteMessageDto messageDto = new InSiteMessageDto();
        BeanUtils.copyProperties(messageBean, messageDto);
        messageDto.setTime(sdf.format(messageBean.getCtime()));
        return messageDto;
    }

    @Override
    public List<InSiteMessageDto> getMessagesByUserId(String userId) {
        List<InSiteMessageBean> messageBeans = inSiteMessageRepository.findByUserIdAndIsDel(userId, 0);
        return convertToDtoList(messageBeans);
    }

    @Override
    public List<InSiteMessageDto> getUnreadMessagesByUserId(String userId) {
        List<InSiteMessageBean> messageBeans = inSiteMessageRepository.findByUserIdAndReadAndIsDel(userId, false, 0);
        return convertToDtoList(messageBeans);
    }

    @Override
    public void markAsRead(Long id) {
        inSiteMessageRepository.updateIsReadById(id, true);
    }

    @Override
    public void markAllAsRead(String userId) {
        inSiteMessageRepository.updateIsReadByUserId(userId, true);
    }

    private List<InSiteMessageDto> convertToDtoList(List<InSiteMessageBean> messageBeans) {
        List<InSiteMessageDto> messageDtos = new ArrayList<>();
        for (InSiteMessageBean messageBean : messageBeans) {
            InSiteMessageDto messageDto = new InSiteMessageDto();
            BeanUtils.copyProperties(messageBean, messageDto);
            messageDto.setRead(messageBean.getRead());
            messageDto.setTime(sdf.format(messageBean.getCtime()));
            messageDtos.add(messageDto);
        }
        return messageDtos;
    }
}
