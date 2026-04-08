package com.datalinkx.dataserver.service;


import com.datalinkx.dataserver.bean.bo.AlarmRuleBo;
import com.datalinkx.dataserver.bean.dto.InSiteMessageDto;
import com.datalinkx.dataserver.bean.vo.AlarmVo;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.AlarmForm;

import java.util.List;

public interface AlarmService {
    String create(AlarmForm.AlarmCreateForm createAlarmComponentForm);

    String modify(AlarmForm.AlarmModifyForm alarmModifyForm);

    List<AlarmVo.AlarmInfoVo> list();

    void delete(String alarmId);

    AlarmVo.AlarmInfoVo info(String alarmId);

    List<AlarmRuleBo.ListBo> ruleList(AlarmForm.RuleListForm alarmListForm);

    void ruleDelete(String ruleId);

    String ruleCreate(AlarmForm.RuleCreateForm ruleCreateForm);

    AlarmRuleBo.ListBo ruleInfo(String ruleId);

    String ruleModify(AlarmForm.RuleModifyForm ruleModifyForm);

    void shutdown(String ruleId);
    
    // 站内信相关方法
    PageVo<List<InSiteMessageDto>> getInSiteMessages(String userId, Integer page, Integer size);
    void markInSiteMessageAsRead(Long messageId, String userId);
    void markAllInSiteMessagesAsRead(String userId);
}
