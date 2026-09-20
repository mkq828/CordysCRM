package cn.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 租户开通时从默认组织(100001)复制阶段配置。
 * <p>
 * 阶段配置的 id 是语义键（CREATE/SUCCESS/FAIL 等），被前后端代码硬编码引用，
 * 因此复制时保留原始 id，仅替换 organization_id。
 * </p>
 */
public interface ExtTenantConfigMapper {

    int copyOpportunityStageConfig(@Param("orgId") String orgId);

    int copyContractStageConfig(@Param("orgId") String orgId);

    int copyOrderStageConfig(@Param("orgId") String orgId);
}
