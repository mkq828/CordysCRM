package cn.cordys.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 敏感数据加密工具（第三方 API Key、回调密钥、用户密钥等）。
 *
 * <p>采用 AES-256-GCM 认证加密，每次加密使用随机 12 字节 IV（杜绝固定 IV 复用风险），
 * 密文带 {@code enc:v1:} 版本前缀，便于后续密钥轮换与向后兼容。
 * 主密钥由环境变量 {@code CORDYS_CRYPTO_MASTER_KEY} 或系统属性 {@code cordys.crypto.master-key} 注入；
 * 未配置时加密降级为「原样返回」（仅开发环境，生产必须配置，否则密钥仍为明文存储）。</p>
 *
 * <p>解密对历史明文数据做「惰性升级」：无 {@code enc:v1:} 前缀的值直接原样返回，保证存量数据兼容，
 * 待业务重新保存后自然升级为密文。</p>
 */
@Slf4j
public final class SecretCipher {

    /** 密文版本前缀，用于识别已加密值，避免重复加密 */
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final String ENV_KEY = "CORDYS_CRYPTO_MASTER_KEY";
    private static final String SYS_KEY = "cordys.crypto.master-key";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    private SecretCipher() {
    }

    /** 判断是否已是密文（带版本前缀） */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /**
     * 加密敏感值。空值返回 null，已加密值幂等返回。
     * 主密钥未配置时降级为原样返回（仅开发环境）。
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        if (isEncrypted(plaintext)) {
            return plaintext;
        }
        String masterKey = resolveMasterKey();
        if (masterKey == null) {
            return plaintext;
        }
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        byte[] cipherBytes = gcm(Cipher.ENCRYPT_MODE, plaintext.getBytes(StandardCharsets.UTF_8), iv, masterKey);
        byte[] payload = new byte[IV_LENGTH + cipherBytes.length];
        System.arraycopy(iv, 0, payload, 0, IV_LENGTH);
        System.arraycopy(cipherBytes, 0, payload, IV_LENGTH, cipherBytes.length);
        return PREFIX + Base64.getEncoder().encodeToString(payload);
    }

    /**
     * 解密敏感值。空值返回 null；无版本前缀的明文（历史数据）原样返回，保证向后兼容。
     * 解密失败（主密钥不匹配）抛出运行时异常，便于第一时间暴露配置错误。
     */
    public static String decrypt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (!isEncrypted(value)) {
            return value;
        }
        String masterKey = resolveMasterKey();
        if (masterKey == null) {
            throw new IllegalStateException("检测到密文数据但未配置 CORDYS_CRYPTO_MASTER_KEY，无法解密");
        }
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("密文格式非法：" + value, e);
        }
        if (payload.length < IV_LENGTH) {
            throw new IllegalStateException("密文长度非法：" + value);
        }
        byte[] iv = new byte[IV_LENGTH];
        byte[] cipherBytes = new byte[payload.length - IV_LENGTH];
        System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
        System.arraycopy(payload, IV_LENGTH, cipherBytes, 0, cipherBytes.length);
        return new String(gcm(Cipher.DECRYPT_MODE, cipherBytes, iv, masterKey), StandardCharsets.UTF_8);
    }

    private static byte[] gcm(int mode, byte[] input, byte[] iv, String masterKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(aesKey(masterKey), "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new IllegalStateException("密钥加解密失败，请检查 CORDYS_CRYPTO_MASTER_KEY 配置是否一致", e);
        }
    }

    /** 任意长度口令经 SHA-256 派生为 32 字节 AES-256 密钥 */
    private static byte[] aesKey(String masterKey) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("主密钥派生失败", e);
        }
    }

    /** 解析主密钥；未配置时告警一次并返回 null（调用方降级处理） */
    private static String resolveMasterKey() {
        String key = System.getenv(ENV_KEY);
        if (key == null || key.isBlank()) {
            key = System.getProperty(SYS_KEY);
        }
        if (key == null || key.isBlank()) {
            if (WARNED.compareAndSet(false, true)) {
                log.warn("未配置加密主密钥（环境变量 {} / 系统属性 {}），敏感数据加密降级为明文存储，生产环境必须配置！",
                        ENV_KEY, SYS_KEY);
            }
            return null;
        }
        return key;
    }
}
