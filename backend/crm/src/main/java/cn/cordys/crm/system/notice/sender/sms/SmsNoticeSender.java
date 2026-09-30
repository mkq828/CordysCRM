package cn.cordys.crm.system.notice.sender.sms;

import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.notice.sender.AbstractNoticeSender;
import cn.cordys.crm.system.utils.SmsSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 站内通知的短信通道分发器：渲染模板 → 解析接收人手机号 → 调 {@link SmsSender}。
 * 仿 {@link cn.cordys.crm.system.notice.sender.mail.MailNoticeSender}。
 */
@Component
@Slf4j
public class SmsNoticeSender extends AbstractNoticeSender {

    @Resource
    private SmsSender smsSender;

    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        try {
            String context = super.getContext(messageDetailDTO, noticeModel);
            sendSms(context, noticeModel, messageDetailDTO.getOrganizationId());
            log.debug("发送短信结束");
        } catch (Exception e) {
            log.error("短信消息通知失败：{}", String.valueOf(e));
        }
    }

    /**
     * 供 {@link cn.cordys.crm.system.notice.NoticeSendService} 反射获取后按通道分发调用的入口，仿 wecom 分支。
     */
    public void sendSms(MessageDetailDTO clonedMessageDetail, NoticeModel clonedNoticeModel) {
        this.send(clonedMessageDetail, clonedNoticeModel);
    }

    private void sendSms(String context, NoticeModel noticeModel, String organizationId) {
        List<Receiver> receivers = super.getReceivers(noticeModel.getReceivers(), noticeModel.isExcludeSelf(), noticeModel.getOperator());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        List<String> userIds = receivers.stream()
                .map(Receiver::getUserId)
                .distinct()
                .toList();
        List<String> phones = super.getUsers(userIds, organizationId).stream()
                .map(User::getPhone)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(phones)) {
            log.warn("接收人无手机号，无法发送短信");
            return;
        }
        phones.forEach(phone -> smsSender.send(phone, context));
    }
}
