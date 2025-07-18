package com.datalinkx.messagehub.transmitter;

import com.datalinkx.common.constants.MessageHubConstants;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.messagehub.bean.dto.AlarmMessageDto;
import com.datalinkx.messagehub.bean.form.ProducerAdapterForm;
import com.datalinkx.messagehub.service.MessageHubService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AlarmProduceTransmitter {

    @Resource(name = "messageHubServiceImpl")
    MessageHubService messageHubService;

    public void pushAlarmMessage(String jobId, Integer status, String errorMsg) {
        ProducerAdapterForm producerAdapterForm = new ProducerAdapterForm();
        producerAdapterForm.setType(MessageHubConstants.REDIS_STREAM_TYPE);
        producerAdapterForm.setTopic(MessageHubConstants.JOB_ALARM_PUSH);
        producerAdapterForm.setGroup(MessageHubConstants.GLOBAL_COMMON_GROUP);
        AlarmMessageDto jobProgress = AlarmMessageDto.builder()
                .jobId(jobId)
                .status(status)
                .errmsg(errorMsg)
                .build();
        producerAdapterForm.setMessage(JsonUtils.toJson(jobProgress));
        messageHubService.produce(producerAdapterForm);
    }
}
