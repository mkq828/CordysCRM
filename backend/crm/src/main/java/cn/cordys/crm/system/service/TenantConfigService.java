package cn.cordys.crm.system.service;

import cn.cordys.crm.contract.mapper.ExtContractStageConfigMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.order.mapper.ExtOrderStageConfigMapper;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.mapper.ExtTenantConfigMapper;
import cn.cordys.mybatis.DataAccessLayer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户开通时的组织级配置播种。
 * <p>
 * 默认组织 100001 的配置（阶段配置等）在 Flyway 迁移中只播种一次，新租户（及存量回填租户）
 * 需要各自持有可独立定制的一份。阶段配置的 id 是语义键，复制时保留 id、仅替换 organization_id。
 * </p>
 */
@Service
public class TenantConfigService {

    private static final String DEFAULT_ORG = "100001";

    @Resource
    private ExtOpportunityStageConfigMapper extOpportunityStageConfigMapper;

    @Resource
    private ExtContractStageConfigMapper extContractStageConfigMapper;

    @Resource
    private ExtOrderStageConfigMapper extOrderStageConfigMapper;

    @Resource
    private ExtTenantConfigMapper extTenantConfigMapper;

    /**
     * 为指定组织播种阶段配置（商机/合同/订单），已有配置时跳过。
     *
     * @param orgId 组织ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void initStageConfigs(String orgId) {
        if (orgId == null || DEFAULT_ORG.equals(orgId)) {
            return;
        }
        if (extOpportunityStageConfigMapper.countStageConfig(orgId) == 0) {
            extTenantConfigMapper.copyOpportunityStageConfig(orgId);
        }
        if (extContractStageConfigMapper.countStageConfig(orgId) == 0) {
            extTenantConfigMapper.copyContractStageConfig(orgId);
        }
        if (extOrderStageConfigMapper.countStageConfig(orgId) == 0) {
            extTenantConfigMapper.copyOrderStageConfig(orgId);
        }
    }

    /**
     * 回填存量租户缺失的阶段配置（幂等）。
     */
    public void backfillTenantStageConfigs() {
        List<Organization> organizations = DataAccessLayer.with(Organization.class).selectAll(null);
        for (Organization organization : organizations) {
            initStageConfigs(organization.getId());
        }
    }
}
