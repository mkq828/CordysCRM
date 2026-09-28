package cn.cordys.crm.platform.util;

import cn.cordys.common.constants.InternalRole;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.security.SessionUser;
import cn.cordys.security.SessionUtils;
import org.apache.commons.lang3.Strings;

/**
 * 平台侧会话判定工具：区分 admin / 城市经理，用于 Service 层数据范围过滤。
 * <p>
 * 判断依据与 {@code UserLoginService} 的 {@code isPlatformUser} 一致：admin 按 userId 短路，
 * 城市经理按登录时加载进 Session 的角色（internal=1）判定，避免每次请求回查 sys_user_role。
 */
public final class PlatformSessionUtils {

    private PlatformSessionUtils() {
    }

    /**
     * 当前用户是否为平台超级管理员（userId == admin）。
     */
    public static boolean isAdmin() {
        SessionUser user = SessionUtils.getUser();
        return user != null && Strings.CS.equals(InternalUser.ADMIN.getValue(), user.getId());
    }

    /**
     * 当前用户是否拥有城市经理角色。
     */
    public static boolean isCityManager() {
        SessionUser user = SessionUtils.getUser();
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> InternalRole.CITY_MANAGER.getValue().equals(role.getId()));
    }
}
