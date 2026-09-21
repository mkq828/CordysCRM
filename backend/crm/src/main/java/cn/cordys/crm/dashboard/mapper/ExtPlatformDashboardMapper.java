package cn.cordys.crm.dashboard.mapper;

import cn.cordys.crm.system.domain.Organization;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 平台大屏（admin）跨租户聚合查询。均不带 organization_id 过滤，GROUP BY organization_id。
 */
public interface ExtPlatformDashboardMapper {

    /**
     * 所有租户（含 org_type / create_time）
     */
    List<Organization> selectAllOrganizations();

    /**
     * 各租户账号数（sys_organization_user）
     */
    List<Map<String, Object>> countUsersGroupByOrg();

    /**
     * 各租户客户数（customer）
     */
    List<Map<String, Object>> countCustomersGroupByOrg();

    /**
     * 各租户线索数（clue）
     */
    List<Map<String, Object>> countCluesGroupByOrg();

    /**
     * 各租户商机数（opportunity）
     */
    List<Map<String, Object>> countOpportunitiesGroupByOrg();

    /**
     * 各租户订单数（sales_order）
     */
    List<Map<String, Object>> countOrdersGroupByOrg();

    /**
     * 各租户合同总额（contract.amount，排除作废）
     */
    List<Map<String, Object>> sumContractAmountGroupByOrg();

    /**
     * 各租户已回款（contract_payment_record.record_amount）
     */
    List<Map<String, Object>> sumReceivedAmountGroupByOrg();

    /**
     * 各租户最后登录时间（sys_login_log.operator -> sys_organization_user.user_id）
     */
    List<Map<String, Object>> maxLoginTimeGroupByOrg();

    /**
     * 各租户近一年登录时间（用于计算连续使用天数 / 近30天活跃天数）
     */
    List<Map<String, Object>> selectLoginDaysGroupByOrg(@Param("sinceMillis") long sinceMillis);

    /**
     * 各租户最近一次登录城市
     */
    List<Map<String, Object>> selectLatestLoginCityGroupByOrg();

    /**
     * 各租户近30天主要登录城市（登录次数最多的城市）
     */
    List<Map<String, Object>> selectTopLoginCityGroupByOrg(@Param("sinceMillis") long sinceMillis);
}
