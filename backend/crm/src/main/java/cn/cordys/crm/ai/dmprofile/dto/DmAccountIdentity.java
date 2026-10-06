package cn.cordys.crm.ai.dmprofile.dto;

/**
 * 绑定登录态时从采集服务带回的「自己」账号身份（昵称 + 抖音号），用于回填 ai_dm_account。
 * 采集服务读不到时字段为 null，调用方应跳过空值避免覆盖已有数据。
 */
public record DmAccountIdentity(String accountName, String uniqueId) {
}
