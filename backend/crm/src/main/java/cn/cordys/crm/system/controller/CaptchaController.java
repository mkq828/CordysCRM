package cn.cordys.crm.system.controller;

import cn.cordys.crm.system.dto.response.CaptchaResponse;
import cn.cordys.crm.system.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码控制器，用于登录、注册等场景获取图形验证码。
 */
@RestController
@RequestMapping
@Tag(name = "验证码")
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    /**
     * 获取图形验证码。
     *
     * @return 验证码 ID 与 base64 图片
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码")
    public CaptchaResponse captcha() {
        return captchaService.generate();
    }
}
