package cn.cordys.crm.system.utils;

import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 短信发送骨架（阿里云）。
 * <p>
 * 第一版只落骨架：从 sys_parameter 读平台级凭据（sms.accessKeyId / sms.accessKeySecret / sms.signName），
 * 未配置时跳过并打日志；上线前接入阿里云 dysmsapi 后，在 {@link #send} 里补真实 SendSmsRequest 调用。
 * </p>
 */
@Component
@Slf4j
public class SmsSender {

    private static final String PARAM_ACCESS_KEY_ID = "sms.accessKeyId";
    private static final String PARAM_ACCESS_KEY_SECRET = "sms.accessKeySecret";
    private static final String PARAM_SIGN_NAME = "sms.signName";

    @Resource
    private BaseMapper<Parameter> parameterMapper;

    /**
     * 发送短信（骨架：凭据未配置时仅记录日志，不真正发送）
     *
     * @param phone   接收手机号
     * @param content 短信正文
     */
    public void send(String phone, String content) {
        if (StringUtils.isBlank(phone)) {
            return;
        }
        String accessKeyId = getParam(PARAM_ACCESS_KEY_ID);
        String accessKeySecret = getParam(PARAM_ACCESS_KEY_SECRET);
        String signName = getParam(PARAM_SIGN_NAME);
        if (StringUtils.isAnyBlank(accessKeyId, accessKeySecret, signName)) {
            log.info("短信通道未配置（sms.accessKeyId/accessKeySecret/signName），跳过发送，收件人：{}", phone);
            return;
        }
        // TODO 上线前接入阿里云 dysmsapi：构造 SendSmsRequest（SignName/TemplateCode/PhoneNumbers/TemplateParam）并调用
        log.info("短信骨架发送：to={}, signName={}, content={}", phone, signName, content);
    }

    private String getParam(String key) {
        Parameter parameter = parameterMapper.selectByPrimaryKey(key);
        return parameter == null ? null : parameter.getParamValue();
    }
}
