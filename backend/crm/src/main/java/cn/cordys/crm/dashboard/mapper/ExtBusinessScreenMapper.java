package cn.cordys.crm.dashboard.mapper;

import cn.cordys.crm.dashboard.dto.response.BusinessScreenKpi;
import cn.cordys.crm.dashboard.dto.response.CustomerBrief;
import cn.cordys.crm.dashboard.dto.response.DistributionItem;
import cn.cordys.crm.dashboard.dto.response.RankItem;
import cn.cordys.crm.dashboard.dto.response.RegionItem;
import cn.cordys.crm.dashboard.dto.response.RegionKpi;
import cn.cordys.crm.dashboard.dto.response.SalesKpi;
import cn.cordys.crm.dashboard.dto.response.StageContractItem;
import cn.cordys.crm.dashboard.dto.response.TrendPoint;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 经营大屏（租户视角）聚合查询。均以 organization_id 兜底隔离，老板全局视角（不按 owner 过滤）。
 */
public interface ExtBusinessScreenMapper {

    /**
     * 核心指标（客户/线索/商机/订单/合同数 + 合同金额 + 已核销回款 + 本月新增）。
     *
     * @param orgId      组织ID
     * @param monthStart 本月起始毫秒时间戳（本月新增统计用）
     */
    BusinessScreenKpi selectKpi(@Param("orgId") String orgId, @Param("monthStart") long monthStart);

    /**
     * 近 N 月合同金额趋势（不含作废）。
     */
    List<TrendPoint> selectContractTrend(@Param("orgId") String orgId, @Param("sinceMillis") long sinceMillis);

    /**
     * 近 N 月已核销回款趋势。
     */
    List<TrendPoint> selectPaymentTrend(@Param("orgId") String orgId, @Param("sinceMillis") long sinceMillis);

    /**
     * 销售业绩排行（按负责人聚合合同金额/合同数/已核销回款/客户数，取合同金额 TOP）。
     */
    List<RankItem> selectSalesRank(@Param("orgId") String orgId);

    /**
     * 合同阶段分布（不含作废；阶段名经 contract_stage_config 解析，未配置则回退展示阶段编码）。
     */
    List<DistributionItem> selectContractStageDist(@Param("orgId") String orgId);

    /**
     * 客户省份分布。客户地址为自定义字段 customerArea（LOCATION，存区县 adcode），
     * 按省级 adcode 聚合（LEFT(field_value,2) + '0000'）。
     */
    List<RegionItem> selectCustomerRegionDist(@Param("orgId") String orgId);

    /**
     * 销售个人核心指标（按负责人）。
     */
    SalesKpi selectSalesKpi(@Param("orgId") String orgId, @Param("ownerId") String ownerId, @Param("monthStart") long monthStart);

    /**
     * 销售近 N 月合同金额趋势。
     */
    List<TrendPoint> selectSalesContractTrend(@Param("orgId") String orgId, @Param("ownerId") String ownerId, @Param("sinceMillis") long sinceMillis);

    /**
     * 销售近 N 月已核销回款趋势。
     */
    List<TrendPoint> selectSalesPaymentTrend(@Param("orgId") String orgId, @Param("ownerId") String ownerId, @Param("sinceMillis") long sinceMillis);

    /**
     * 销售合同阶段分布。
     */
    List<DistributionItem> selectSalesStageDist(@Param("orgId") String orgId, @Param("ownerId") String ownerId);

    /**
     * 销售名下客户列表（含合同额、已核销回款）。
     */
    List<CustomerBrief> selectSalesCustomerList(@Param("orgId") String orgId, @Param("ownerId") String ownerId);

    /**
     * 区域核心指标（按省级 adcode）。
     */
    RegionKpi selectRegionKpi(@Param("orgId") String orgId, @Param("adcode") String adcode);

    /**
     * 区域客户列表（含合同额、已核销回款）。
     */
    List<CustomerBrief> selectRegionCustomerList(@Param("orgId") String orgId, @Param("adcode") String adcode);

    /**
     * 区域销售分布（按负责人聚合合同/回款/客户）。
     */
    List<RankItem> selectRegionSalesDist(@Param("orgId") String orgId, @Param("adcode") String adcode);

    /**
     * 阶段合同列表。
     */
    List<StageContractItem> selectStageContractList(@Param("orgId") String orgId, @Param("stage") String stage);

    /**
     * 阶段名称（未配置返回 null，服务层回退展示阶段编码）。
     */
    String selectStageName(@Param("orgId") String orgId, @Param("stage") String stage);
}
