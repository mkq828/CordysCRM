package cn.cordys.common.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.apache.commons.lang3.StringUtils;

/**
 * 密码哈希工具，统一使用 bcrypt 存储与校验密码。
 * <p>
 * 兼容历史 MD5 密文的校验（登录时惰性升级为 bcrypt）。
 * </p>
 */
public final class PasswordUtils {

    /** bcrypt 成本因子，数值越高越慢、越抗暴力破解 */
    private static final int BCRYPT_COST = 10;

    /** bcrypt 密文前缀（历史 MD5 为 32 位十六进制，不会以 $2 开头） */
    private static final String BCRYPT_PREFIX = "$2";

    /** 新增员工 / 重置密码时使用的默认初始密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    private PasswordUtils() {
    }

    /**
     * 生成 bcrypt 密文
     *
     * @param rawPassword 明文密码
     *
     * @return bcrypt 密文
     */
    public static String encode(String rawPassword) {
        if (StringUtils.isBlank(rawPassword)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, rawPassword.toCharArray());
    }

    /**
     * 校验密码是否匹配（自动兼容历史 MD5 密文）
     *
     * @param rawPassword 明文密码
     * @param stored      数据库中存储的密文
     *
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String stored) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(stored)) {
            return false;
        }
        if (isBcrypt(stored)) {
            return BCrypt.verifyer().verify(rawPassword.toCharArray(), stored).verified;
        }
        return CodingUtils.md5(rawPassword).equalsIgnoreCase(stored);
    }

    /**
     * 是否为 bcrypt 密文
     *
     * @param stored 存储的密文
     *
     * @return true 表示 bcrypt，false 表示历史 MD5
     */
    public static boolean isBcrypt(String stored) {
        return stored != null && stored.startsWith(BCRYPT_PREFIX);
    }
}
