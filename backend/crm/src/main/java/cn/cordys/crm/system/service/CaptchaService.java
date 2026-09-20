package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.dto.response.CaptchaResponse;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务
 * <p>
 * 使用 Java2D 手绘验证码图片（无第三方依赖），答案存 Redis 并设置过期时间，校验后一次性失效。
 * </p>
 */
@Service
public class CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha_code:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    // 去掉易混淆字符 0/O/1/l/I
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成验证码
     *
     * @return 验证码 ID 与 base64 图片
     */
    public CaptchaResponse generate() {
        String code = generateCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + captchaId, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaId(captchaId);
        response.setCaptchaImage(drawImage(code));
        return response;
    }

    /**
     * 校验验证码（不区分大小写，校验后一次性失效）
     *
     * @param captchaId   验证码 ID
     * @param captchaCode 用户输入的验证码
     */
    public void validate(String captchaId, String captchaCode) {
        if (StringUtils.isBlank(captchaId) || StringUtils.isBlank(captchaCode)) {
            throw new GenericException(Translator.get("captcha_error"));
        }
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String answer = stringRedisTemplate.opsForValue().get(key);
        if (answer == null || !Strings.CI.equals(answer, StringUtils.trim(captchaCode))) {
            throw new GenericException(Translator.get("captcha_error"));
        }
        // 一次性使用，校验后删除
        stringRedisTemplate.delete(key);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String drawImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 背景
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setColor(new Color(180, 190, 210));
            for (int i = 0; i < 6; i++) {
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
            // 干扰点
            for (int i = 0; i < 60; i++) {
                g.setColor(new Color(150 + random.nextInt(80), 160 + random.nextInt(80), 190 + random.nextInt(60)));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
            }

            // 绘制字符
            int charWidth = WIDTH / (CODE_LENGTH + 1);
            for (int i = 0; i < code.length(); i++) {
                String ch = String.valueOf(code.charAt(i));
                Font font = new Font("SansSerif", Font.BOLD, 24 + random.nextInt(6));
                g.setFont(font);
                g.setColor(new Color(30 + random.nextInt(60), 70 + random.nextInt(60), 150 + random.nextInt(60)));
                AffineTransform old = g.getTransform();
                double angle = (random.nextDouble() - 0.5) * 0.6;
                int x = charWidth * (i + 1);
                int y = 28 + random.nextInt(6);
                g.rotate(angle, x, y);
                g.drawString(ch, x, y);
                g.setTransform(old);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new GenericException("生成验证码失败", e);
        }
    }
}
