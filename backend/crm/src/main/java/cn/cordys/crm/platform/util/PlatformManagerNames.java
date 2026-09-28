package cn.cordys.crm.platform.util;

import cn.cordys.crm.platform.domain.PlatformCityManager;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.mybatis.DataAccessLayer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 平台经理名字解析：签约/跟进经理以租户表 sys_organization 为口径。
 * 签约经理永久、跟进经理取当前值（二次分配后仍准确），统一从租户表解析。
 */
public final class PlatformManagerNames {

    public record ManagerNames(String signManagerName, String followManagerName) {
    }

    private PlatformManagerNames() {
    }

    /**
     * 给定租户组织 ID 集合，返回 orgId -> 签约/跟进经理名字。
     */
    public static Map<String, ManagerNames> resolve(Collection<String> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Map.of();
        }
        List<String> distinctOrgIds = orgIds.stream().filter(StringUtils::isNotBlank).distinct().toList();
        if (distinctOrgIds.isEmpty()) {
            return Map.of();
        }
        List<Organization> orgs = DataAccessLayer.with(Organization.class).selectByIds(distinctOrgIds);
        Set<String> managerIds = new HashSet<>();
        for (Organization o : orgs) {
            if (StringUtils.isNotBlank(o.getSignManagerId())) {
                managerIds.add(o.getSignManagerId());
            }
            if (StringUtils.isNotBlank(o.getFollowManagerId())) {
                managerIds.add(o.getFollowManagerId());
            }
        }
        Map<String, String> nameMap = managerIds.isEmpty() ? Map.of()
                : DataAccessLayer.with(PlatformCityManager.class).selectByIds(new ArrayList<>(managerIds)).stream()
                        .collect(Collectors.toMap(PlatformCityManager::getId, PlatformCityManager::getName, (a, b) -> a));
        Map<String, ManagerNames> result = new HashMap<>();
        for (Organization o : orgs) {
            result.put(o.getId(), new ManagerNames(
                    nameMap.get(o.getSignManagerId()),
                    nameMap.get(o.getFollowManagerId())));
        }
        return result;
    }
}
