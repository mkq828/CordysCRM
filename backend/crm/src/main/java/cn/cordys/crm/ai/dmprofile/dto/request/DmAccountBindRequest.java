package cn.cordys.crm.ai.dmprofile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 绑定私信平台账号请求。主路径走 Cookie 迁移：租户在真实浏览器登录后粘贴 Cookie。
 */
@Data
public class DmAccountBindRequest {

    @Schema(description = "平台: douyin/kuaishou/xiaohongshu")
    @NotBlank(message = "平台不能为空")
    private String platform;

    @Schema(description = "登录 Cookie 串(需含 sessionid_ss 与 uid_tt)")
    @NotBlank(message = "Cookie 不能为空")
    private String cookieText;
}
