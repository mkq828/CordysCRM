package cn.cordys.crm.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 协议勾选留痕日志：注册页主动勾选《SaaS 服务协议 / 知识产权与保密 / 隐私政策》的动作记录。
 */
@Data
@Table(name = "sys_agreement_consent_log")
public class AgreementConsentLog {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "用户ID（企业注册审核通过前为空）")
    private String userId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "协议类型")
    private String agreementType;

    @Schema(description = "协议版本")
    private String agreementVersion;

    @Schema(description = "客户端IP")
    private String ip;

    @Schema(description = "浏览器User-Agent")
    private String userAgent;

    @Schema(description = "勾选时间")
    private Long createTime;
}
