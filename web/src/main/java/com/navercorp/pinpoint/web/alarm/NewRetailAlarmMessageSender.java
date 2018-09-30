package com.navercorp.pinpoint.web.alarm;

import com.alibaba.fastjson.JSON;
import com.navercorp.pinpoint.web.alarm.checker.AlarmChecker;
import com.navercorp.pinpoint.web.service.UserGroupService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NewRetailAlarmMessageSender implements AlarmMessageSender {
  private final static Logger logger = LoggerFactory.getLogger("NewRetailAlarmMessageSender");

  @Autowired
  private UserGroupService userGroupService;
  @Override
  public void sendSms(AlarmChecker checker, int sequenceCount) {
    try {
      List<String> phoneList = userGroupService.selectPhoneNumberOfMember(checker.getuserGroupId());
      if (phoneList == null || phoneList.size() == 0) {
        return;
      }

      logger.info(buildMessage(checker, "sms", phoneList));
    }catch (Exception e){
      e.printStackTrace();
    }

  }

  @Override
  public void sendEmail(AlarmChecker checker, int sequenceCount) {
    try {
      List<String> emailList = userGroupService.selectEmailOfMember(checker.getuserGroupId());
      if (emailList == null || emailList.size() == 0) {
        return;
      }
      logger.info(buildMessage(checker, "email", emailList));
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  private String buildMessage(AlarmChecker checker,String type,List<String> addressList){
    Map<String,Object> msg = new HashMap<>();
    msg.put("rule",checker.getRule());
    msg.put("detectedMsg",checker.getDetectedValueImpl());
    msg.put("addressList",addressList);
    msg.put("type",type);
    msg.put("unit",checker.getUnit());
    msg.put("groupId",checker.getuserGroupId());
    return JSON.toJSONString(msg);
  }

}
