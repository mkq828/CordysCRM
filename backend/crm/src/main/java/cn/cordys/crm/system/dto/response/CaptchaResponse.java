package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 图形验证码响应
 */
@Getter
@Setter
public class CaptchaResponse {

    @Schema(description = "验证码唯一标识")
    private String captchaId;

    @Schema(description = "验证码图片（base64 data URI）")
    private String captchaImage;
}
