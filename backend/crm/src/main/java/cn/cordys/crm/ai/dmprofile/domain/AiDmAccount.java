package cn.cordys.crm.ai.dmprofile.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 私信获客画像（功能 8）：租户绑定的平台账号 + 登录态 Cookie 密文。
 * Cookie 是登录凭据，明文只在绑定瞬间内存中使用，落库一律加密；解绑即清空。
 */
@Data
@Table(name = "ai_dm_account")
public class AiDmAccount extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "平台: douyin/kuaishou/xiaohongshu")
    private String platform;

    @Schema(description = "账号昵称(绑定后回填)")
    private String accountName;

    @Schema(description = "账号ID(抖音号)")
    private String uniqueId;

    @Schema(description = "登录态Cookie密文(EncryptUtils.aesEncrypt)")
    private String cookieCiphertext;

    @Schema(description = "绑定状态: LOGGED_IN/CRAWLING/EXPIRED/FAILED")
    private String status;

    @Schema(description = "最近采集时间(毫秒)")
    private Long lastCrawlTime;

    @Schema(description = "最近失败原因")
    private String lastError;
}
