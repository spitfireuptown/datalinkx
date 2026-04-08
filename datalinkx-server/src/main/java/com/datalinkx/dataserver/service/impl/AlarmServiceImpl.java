package com.datalinkx.dataserver.service.impl;

import com.datalinkx.common.exception.DatalinkXServerException;
import com.datalinkx.common.result.StatusCode;
import com.datalinkx.common.utils.IdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.ObjectUtils;
import com.datalinkx.dataserver.bean.bo.AlarmRuleBo;
import com.datalinkx.dataserver.bean.domain.*;
import com.datalinkx.dataserver.bean.dto.AlarmRuleDto;
import com.datalinkx.dataserver.bean.dto.InSiteMessageDto;
import com.datalinkx.dataserver.bean.vo.AlarmVo;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.AlarmForm;
import com.datalinkx.dataserver.repository.AlarmRepository;
import com.datalinkx.dataserver.repository.AlarmRuleRepository;
import com.datalinkx.dataserver.repository.InSiteMessageRepository;
import com.datalinkx.dataserver.repository.JobRepository;
import com.datalinkx.dataserver.service.AlarmService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlarmServiceImpl implements AlarmService {

    @Autowired
    AlarmRepository alarmRepository;
    @Autowired
    AlarmRuleRepository alarmRuleRepository;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    InSiteMessageRepository inSiteMessageRepository;
    
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    @Override
    public String create(AlarmForm.AlarmCreateForm createAlarmComponentForm) {
        AlarmComponentBean alarmComponentBean = new AlarmComponentBean();
        BeanUtils.copyProperties(createAlarmComponentForm, alarmComponentBean);
        String alarmId = IdUtils.genKey("alarm");
        alarmComponentBean.setAlarmId(alarmId);
        alarmRepository.save(alarmComponentBean);
        return alarmId;
    }

    @Override
    public String modify(AlarmForm.AlarmModifyForm modifyForm) {
        AlarmComponentBean alarmComponentBean = alarmRepository.findByAlarmId(modifyForm.getAlarmId()).orElseThrow(() -> new DatalinkXServerException("告警组件不存在"));
        BeanUtils.copyProperties(modifyForm, alarmComponentBean);
        alarmRepository.save(alarmComponentBean);
        return modifyForm.getAlarmId();
    }

    @Override
    public List<AlarmVo.AlarmInfoVo> list() {
        return alarmRepository.findAll().stream().map(alarmComponentBean -> {
            AlarmVo.AlarmInfoVo alarmListVo = new AlarmVo.AlarmInfoVo();
            alarmListVo.setAlarmId(alarmComponentBean.getAlarmId());
            alarmListVo.setType(alarmComponentBean.getType());
            alarmListVo.setName(alarmComponentBean.getName());
            return alarmListVo;
        }).collect(Collectors.toList());
    }

    @Override
    public void delete(String alarmId) {
        alarmRepository.delete(alarmId);
    }

    @Override
    public AlarmVo.AlarmInfoVo info(String alarmId) {
        AlarmComponentBean alarmComponentBean = alarmRepository.findByAlarmId(alarmId)
                .orElseThrow(() -> new DatalinkXServerException(StatusCode.ALARM_CONFIG_NOT_EXISTS, "告警配置未发现"));

        AlarmRuleDto.AlarmConfig alarmConfig = JsonUtils.toObject(alarmComponentBean.getConfig(), AlarmRuleDto.AlarmConfig.class);
        return AlarmVo.AlarmInfoVo.builder()
                .alarmId(alarmComponentBean.getAlarmId())
                .type(alarmComponentBean.getType())
                .name(alarmComponentBean.getName())
                .config(alarmConfig)
                .build();
    }

    @Override
    public List<AlarmRuleBo.ListBo> ruleList(AlarmForm.RuleListForm alarmListForm) {
        AlarmRuleDto.ListDto listDto = new AlarmRuleDto.ListDto();

        if (!ObjectUtils.isEmpty(alarmListForm.getComponentName())) {
            
            List<String> alarmIds = alarmRepository.findAllByNameLike(alarmListForm.getComponentName())
                    .stream().map(AlarmComponentBean::getAlarmId).collect(Collectors.toList());
            listDto.setAlarmIds(alarmIds);
        }

        if (!ObjectUtils.isEmpty(alarmListForm.getJobName())) {

            List<String> jobIds = alarmRuleRepository.findAllByNameLike(alarmListForm.getJobName())
                    .stream().map(AlarmRuleBean::getJobId).collect(Collectors.toList());
            listDto.setJobIds(jobIds);
        }
        
        List<String> jobIds = new ArrayList<>();
        List<String> alarmIds = new ArrayList<>();
        List<AlarmRuleBo.ListBo> result = alarmRuleRepository.findAllWithCondition(listDto)
                .stream()
                .map(obj -> JsonUtils.toObject(JsonUtils.toJson(obj), AlarmRuleBo.ListBo.class))
                .peek(listBo -> {
                    jobIds.add(listBo.getJobId());
                    alarmIds.add(listBo.getAlarmId());
                })
                .collect(Collectors.toList());

        Map<String, String> alarmId2Name = alarmRepository.findAllByAlarmIdIn(alarmIds).stream().collect(Collectors.toMap(AlarmComponentBean::getAlarmId, AlarmComponentBean::getName));
        Map<String, String> jobId2Name = jobRepository.findByJobIdIn(jobIds).stream().collect(Collectors.toMap(JobBean::getJobId, JobBean::getName));

        result.forEach(listBo -> {
            listBo.setAlarmComponentName(alarmId2Name.get(listBo.getAlarmId()));
            listBo.setJobName(jobId2Name.get(listBo.getJobId()));
        });
        return result;
    }

    @Override
    public void ruleDelete(String ruleId) {
        alarmRuleRepository.deleteByRuleId(ruleId);
    }

    @Override
    public String ruleCreate(AlarmForm.RuleCreateForm ruleCreateForm) {
        String ruleId = IdUtils.genKey("rule");

        AlarmRuleBean alarmRuleBean = AlarmRuleBean.builder()
                .name(ruleCreateForm.getName())
                .alarmId(ruleCreateForm.getAlarmId())
                .jobId(ruleCreateForm.getJobId())
                .ruleId(ruleId)
                .type(ruleCreateForm.getType())
                .status(ruleCreateForm.getStatus())
                .build();

        alarmRuleRepository.save(alarmRuleBean);

        return ruleId;
    }

    @Override
    public AlarmRuleBo.ListBo ruleInfo(String ruleId) {
        AlarmRuleBean alarmRuleBean = alarmRuleRepository.findByRuleId(ruleId).orElseThrow(() -> new DatalinkXServerException(StatusCode.ALARM_CONFIG_NOT_EXISTS, "告警规则不存在"));
        AlarmComponentBean alarmComponentBean = alarmRepository.findByAlarmId(alarmRuleBean.getAlarmId()).orElseThrow(() -> new DatalinkXServerException(StatusCode.ALARM_CONFIG_NOT_EXISTS, "告警组件不存在"));
        JobBean jobBean = jobRepository.findByJobId(alarmRuleBean.getJobId()).orElseThrow(() -> new DatalinkXServerException(StatusCode.JOB_NOT_EXISTS, "关联任务不存在"));

        return AlarmRuleBo.ListBo.builder()
               .ruleId(alarmRuleBean.getRuleId())
               .ruleName(alarmRuleBean.getName())
               .alarmId(alarmRuleBean.getAlarmId())
               .alarmComponentName(alarmComponentBean.getName())
               .jobId(alarmRuleBean.getJobId())
               .jobName(jobBean.getName())
                .status(alarmRuleBean.getStatus())
                .pushTime(alarmRuleBean.getPushTime())
               .type(alarmRuleBean.getType()).build();
    }

    @Override
    public String ruleModify(AlarmForm.RuleModifyForm ruleModifyForm) {
        AlarmRuleBean alarmRuleBean = alarmRuleRepository.findByRuleId(ruleModifyForm.getRuleId()).orElseThrow(() -> new DatalinkXServerException(StatusCode.ALARM_CONFIG_NOT_EXISTS, "告警规则不存在"));
        alarmRuleBean.setAlarmId(ruleModifyForm.getAlarmId());
        alarmRuleBean.setJobId(ruleModifyForm.getJobId());
        alarmRuleBean.setType(ruleModifyForm.getType());
        alarmRuleBean.setStatus(ruleModifyForm.getStatus());
        alarmRuleRepository.save(alarmRuleBean);
        return alarmRuleBean.getRuleId();
    }

    @Override
    public void shutdown(String ruleId) {
        alarmRuleRepository.shutdown(ruleId);
    }

    @Override
    public PageVo<List<InSiteMessageDto>> getInSiteMessages(String userId, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<InSiteMessageBean> inSiteMessageBeans = inSiteMessageRepository.pageQuery(pageRequest, userId);
        PageVo<List<InSiteMessageDto>> result = new PageVo<>();
        result.setPageNo(page);
        result.setPageSize(size);
        result.setData(
                inSiteMessageBeans.getContent().stream().map(messageBean -> {
                    InSiteMessageDto messageDto = new InSiteMessageDto();
                    BeanUtils.copyProperties(messageBean, messageDto);
                    messageDto.setRead(messageBean.getRead());
                    messageDto.setTime(sdf.format(messageBean.getCtime()));
                    return messageDto;
                }).collect(Collectors.toList())
        );
        result.setTotalPage(inSiteMessageBeans.getTotalPages());
        result.setTotal(inSiteMessageBeans.getTotalElements());
        return result;
    }

    @Override
    public void markInSiteMessageAsRead(Long messageId, String userId) {
        if (messageId != null) {
            inSiteMessageRepository.updateIsReadById(messageId, true);
        } else {
            // 不传消息id时，将所有消息置为已读
            inSiteMessageRepository.updateIsReadByUserId(userId, true);
        }
    }

    @Override
    public void markAllInSiteMessagesAsRead(String userId) {
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
