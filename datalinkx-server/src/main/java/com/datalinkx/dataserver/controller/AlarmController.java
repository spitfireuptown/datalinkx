package com.datalinkx.dataserver.controller;

import com.datalinkx.common.result.WebResult;
import com.datalinkx.dataserver.bean.bo.AlarmRuleBo;
import com.datalinkx.dataserver.bean.dto.InSiteMessageDto;
import com.datalinkx.dataserver.bean.vo.AlarmVo;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.AlarmForm;
import com.datalinkx.dataserver.service.AlarmService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/alarm")
@Api(tags = "alarm")
public class AlarmController {

    @Autowired
    AlarmService alarmService;

    @GetMapping("/component/list")
    public List<AlarmVo.AlarmInfoVo> list() {
        return alarmService.list();
    }

    @GetMapping("/component/info/{alarmId}")
    public AlarmVo.AlarmInfoVo info(@PathVariable String alarmId) {
        return alarmService.info(alarmId);
    }

    @PostMapping("/component/create")
    public WebResult<String> createAlarmComponent(@RequestBody AlarmForm.AlarmCreateForm createAlarmComponentForm) {
        return WebResult.of(alarmService.create(createAlarmComponentForm));
    }

    @PostMapping("/component/modify")
    public WebResult<String> modifyAlarmComponent(@RequestBody AlarmForm.AlarmModifyForm alarmModifyForm) {
        return WebResult.of(alarmService.modify(alarmModifyForm));
    }


    @PostMapping("/component/delete/{alarmId}")
    public void delete(@PathVariable String alarmId) {
        alarmService.delete(alarmId);
    }

    @GetMapping("/rule/info/{ruleId}")
    public AlarmRuleBo.ListBo ruleInfo(@PathVariable String ruleId) {
        return alarmService.ruleInfo(ruleId);
    }

    @GetMapping("/rule/list")
    public List<AlarmRuleBo.ListBo> ruleList(AlarmForm.RuleListForm alarmListForm) {
        return alarmService.ruleList(alarmListForm);
    }

    @PostMapping("/rule/create")
    public WebResult<String> ruleCreate(@RequestBody AlarmForm.RuleCreateForm ruleCreateForm) {
        return WebResult.of(alarmService.ruleCreate(ruleCreateForm));
    }

    @PostMapping("/rule/modify")
    public WebResult<String> ruleModify(@RequestBody AlarmForm.RuleModifyForm ruleModifyForm) {
        return WebResult.of(alarmService.ruleModify(ruleModifyForm));
    }

    @PostMapping("/rule/shutdown/{ruleId}")
    public void shutdown(@PathVariable String ruleId) {
        this.alarmService.shutdown(ruleId);
    }

    @PostMapping("/rule/delete/{ruleId}")
    public void ruleDelete(@PathVariable String ruleId) {
        this.alarmService.ruleDelete(ruleId);
    }
    
    // 站内信相关接口
    @GetMapping("/in-site-message/list/{userId}")
    public PageVo<List<InSiteMessageDto>> getInSiteMessages(
            @PathVariable String userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return alarmService.getInSiteMessages(userId, page, size);
    }
    
    @PutMapping("/in-site-message/read/{userId}")
    public void markInSiteMessageAsRead(
            @PathVariable String userId,
            @RequestParam(required = false) Long messageId) {
        alarmService.markInSiteMessageAsRead(messageId, userId);
    }
    
    @PutMapping("/in-site-message/read-all/{userId}")
    public void markAllInSiteMessagesAsRead(@PathVariable String userId) {
        alarmService.markAllInSiteMessagesAsRead(userId);
    }
}
