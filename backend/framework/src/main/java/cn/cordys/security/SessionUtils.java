package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.CommonBeanFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;

import java.time.Duration;
import java.util.Map;

import static cn.cordys.security.SessionConstants.ATTR_USER;


/**
 * Session 工具类，提供操作用户 Session 的常用方法。
 * <p>
 * 包含获取当前用户信息、获取 Session ID、踢除用户等功能。
 * </p>
 */
@Slf4j
public class SessionUtils {

    /** 单点登录：当前会话映射 Redis key 前缀（userId -> 当前 sessionId） */
    private static final String SESSION_CURRENT_KEY_PREFIX = "cordys:session:current:";
    /** 单点登录：被踢标记 Redis key 前缀（userId -> 旧会话已被踢） */
    private static final String SESSION_KICKED_KEY_PREFIX = "cordys:session:kicked:";
    /** 当前会话映射 TTL（秒），对齐 session 超时 */
    private static final long SESSION_CURRENT_TTL_SECONDS = 43200;
    /** 被踢标记 TTL（秒），对齐 session 超时：旧会话被踢后，只要仍在会话生命周期内，任何残留请求都应提示「已在其他设备登录」而非通用的「没权限令牌」 */
    private static final long SESSION_KICKED_TTL_SECONDS = 43200;

    /**
     * 获取当前用户的 ID。
     *
     * @return 当前用户的 ID，如果没有获取到用户信息，则返回 null
     */
    public static String getUserId() {
        SessionUser user = getUser();
        return user == null ? null : user.getId();
    }

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户对象，如果未获取到用户信息，则返回 null
     */
    public static SessionUser getUser() {
        try {
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession();
            return (SessionUser) session.getAttribute(ATTR_USER);
        } catch (Exception e) {
            log.warn("后台获取在线用户失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前 Session 的 ID。
     *
     * @return 当前 Session 的 ID
     */
    public static String getSessionId() {
        try {
            return (String) SecurityUtils.getSubject().getSession().getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 踢除指定用户名的用户（从 Redis 会话中删除）。
     *
     * @param username 用户名
     */
    public static void kickOutUser(String username) {
        // 获取 Redis session 存储库
        RedisIndexedSessionRepository sessionRepository = CommonBeanFactory.getBean(RedisIndexedSessionRepository.class);
        if (sessionRepository == null) {
            return;
        }

        // 根据用户名查找会话
        Map<String, ?> users = sessionRepository.findByPrincipalName(username);
        if (MapUtils.isNotEmpty(users)) {
            // 删除所有与该用户名关联的 session
            users.keySet().forEach(k -> {
                sessionRepository.deleteById(k);
                sessionRepository.getSessionRedisOperations().delete("spring:session:sessions:" + k);
            });
        }
    }

    /**
     * 踢除指定用户（从 Redis 会话中删除）。
     *
     * @param operatorId 操作用户 ID
     * @param kickUserId 被踢用户 ID
     */
    public static void kickOutUser(String operatorId, String kickUserId) {
        // 处理用户会话
        boolean isSelfReset = Strings.CS.equals(operatorId, kickUserId);
        if (isSelfReset) {
            // 当前用户重置自己的密码，直接登出
            SecurityUtils.getSubject().logout();
            // 需要检查是否有其他会话存在
            try {
                SessionUtils.kickOutUser(kickUserId);
            } catch (Exception e) {
                log.error("踢出用户失败: {}", e.getMessage());
            }
        } else {
            // 管理员重置他人密码，踢出该用户
            SessionUtils.kickOutUser(kickUserId);
        }

    }

    /**
     * 将当前用户信息保存到 Session 中。
     *
     * @param sessionUser 当前用户对象
     */
    public static void putUser(SessionUser sessionUser) {
        // 保存用户信息到 Session
        SecurityUtils.getSubject().getSession().setAttribute(ATTR_USER, sessionUser);
        // 保存用户 ID 到 Session
        SecurityUtils.getSubject().getSession().setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, sessionUser.getId());
    }
    
    /**
     * 检查指定用户是否有活跃的 Session (用于 F_A_TOKEN 等独立于 Shiro Session 的访问令牌校验)。
     * <p>
     * 通过 Spring Session 的 principal 索引反查, 避免直接依赖 Shiro SessionId (后者会随 Redis TTL 到期)。
     * </p>
     *
     * @param userId 用户 ID (UserDTO.id)
     *
     * @return 是否有至少一个未过期的 Session
     */
    public static boolean hasActiveSession(String userId) {
        if (userId == null) {
            return false;
        }
        RedisIndexedSessionRepository repo = CommonBeanFactory.getBean(RedisIndexedSessionRepository.class);
        if (repo == null) {
            return false;
        }
        // indexed 模式下 findByPrincipalName 只返回未过期、仍在 principal 索引里的 Session
        Map<String, ?> sessions = repo.findByPrincipalName(userId);
        return MapUtils.isNotEmpty(sessions);
    }

    /**
     * 单点登录：登录成功后记录「userId -> 当前 sessionId」，并踢掉旧会话。
     * <p>
     * 同一账号只能保持一个有效会话：若已存在旧会话且不等于当前会话，则删除旧会话并写入「被踢」标记，
     * 旧会话残留的请求会因此走未认证分支并被 {@link #isKicked(String)} 识别为被踢下线。
     * </p>
     *
     * @param userId           用户 ID（SessionUser.id）
     * @param currentSessionId 当前登录产生的 sessionId
     */
    public static void recordLogin(String userId, String currentSessionId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(currentSessionId)) {
            return;
        }
        StringRedisTemplate redis = CommonBeanFactory.getBean(StringRedisTemplate.class);
        if (redis == null) {
            return;
        }
        String currentKey = SESSION_CURRENT_KEY_PREFIX + userId;
        String oldSessionId = redis.opsForValue().get(currentKey);
        if (StringUtils.isNotBlank(oldSessionId) && !oldSessionId.equals(currentSessionId)) {
            // 精确删除旧会话（按 sessionId，避免误删刚登录的当前会话）
            RedisIndexedSessionRepository repo = CommonBeanFactory.getBean(RedisIndexedSessionRepository.class);
            if (repo != null) {
                repo.deleteById(oldSessionId);
                repo.getSessionRedisOperations().delete("spring:session:sessions:" + oldSessionId);
            }
            // 写被踢标记，供 CsrfFilter 识别旧会话并返回「该账号已在其他设备登录」
            redis.opsForValue().set(SESSION_KICKED_KEY_PREFIX + userId, "1",
                    Duration.ofSeconds(SESSION_KICKED_TTL_SECONDS));
        }
        redis.opsForValue().set(currentKey, currentSessionId, Duration.ofSeconds(SESSION_CURRENT_TTL_SECONDS));
    }

    /**
     * 单点登录：判断指定用户是否已被踢下线（存在「被踢」标记）。
     *
     * @param userId 用户 ID
     *
     * @return true 表示该用户存在被踢标记（旧会话已被新登录会话顶掉）
     */
    public static boolean isKicked(String userId) {
        if (StringUtils.isBlank(userId)) {
            return false;
        }
        StringRedisTemplate redis = CommonBeanFactory.getBean(StringRedisTemplate.class);
        if (redis == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(SESSION_KICKED_KEY_PREFIX + userId));
    }

    /**
     * 单点登录：判断当前请求携带的是否是「已被踢下线」的旧会话凭据。
     * <p>
     * /is-login 走 anon 过滤器链，不经过 {@link cn.cordys.common.security.CsrfFilter}，且旧会话已被删除、
     * Shiro Subject 拿不到 userId。这里从 CSRF token 解密出 userId 再查被踢标记，供登录态校验接口补一次被踢检测。
     * </p>
     *
     * @param request HttpServletRequest
     *
     * @return true 表示该请求携带的是已被新登录会话顶掉的旧凭据
     */
    public static boolean isKickedRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String csrfToken = request.getHeader(SessionConstants.CSRF_TOKEN);
        if (StringUtils.isBlank(csrfToken)) {
            return false;
        }
        try {
            String decrypted = CodingUtils.aesDecrypt(csrfToken, SessionUser.secret, CodingUtils.generateIv());
            String[] parts = StringUtils.split(StringUtils.trimToNull(decrypted), "|");
            if (parts != null && parts.length >= 1) {
                return isKicked(parts[0]);
            }
        } catch (Exception e) {
            // 解密失败按「未踢」处理，避免影响正常未登录流程
        }
        return false;
    }
}
