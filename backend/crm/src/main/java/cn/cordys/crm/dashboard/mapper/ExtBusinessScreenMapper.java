package cn.cordys.crm.dashboard.mapper;

import cn.cordys.crm.dashboard.dto.response.BusinessScreenKpi;
import cn.cordys.crm.dashboard.dto.response.CustomerBrief;
import cn.cordys.crm.dashboard.dto.response.DistributionItem;
import cn.cordys.crm.dashboard.dto.response.RankItem;
import cn.cordys.crm.dashboard.dto.response.RegionItem;
import cn.cordys.crm.dashboard.dto.response.RegionKpi;
import cn.cordys.crm.dashboard.dto.response.SalesKpi;
import cn.cordys.crm.dashboard.dto.response.StageContractItem;
import cn.cordys.crm.dashboard.dto.response.StaleBigCustomerRow;
import cn.cordys.crm.dashboard.dto.response.StaleOwnerRow;
import cn.cordys.crm.dashboard.dto.response.TrendPoint;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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
     * @param startMillis 时间窗口下界毫秒时间戳；0 表示不加下界（累计）
     */
    BusinessScreenKpi selectKpi(@Param("orgId") String orgId, @Param("monthStart") long monthStart,
                                @Param("startMillis") long startMillis);

    /**
     * 合同金额趋势（不含作废），按日/月粒度聚合。
     *
     * @param granularity DAY / MONTH，对应 DATE_FORMAT 格式
     */
    List<TrendPoint> selectContractTrend(@Param("orgId") String orgId, @Param("sinceMillis") long sinceMillis,
                                         @Param("granularity") String granularity);

    /**
     * 已核销回款趋势，按日/月粒度聚合。
     */
    List<TrendPoint> selectPaymentTrend(@Param("orgId") String orgId, @Param("sinceMillis") long sinceMillis,
                                        @Param("granularity") String granularity);

    /**
     * 销售业绩排行（按负责人聚合合同金额/合同数/已核销回款/客户数，取合同金额 TOP）。
     * 仅在时间窗口内统计（startMillis &gt; 0 时生效）。
     */
    List<RankItem> selectSalesRank(@Param("orgId") String orgId, @Param("startMillis") long startMillis);

    /**
     * 合同阶段分布（不含作废；阶段名经 contract_stage_config 解析，未配置则回退展示阶段编码）。
     * 仅在时间窗口内统计（startMillis &gt; 0 时生效）。
     */
    List<DistributionItem> selectContractStageDist(@Param("orgId") String orgId, @Param("startMillis") long startMillis);

    /**
     * 客户省份分布。客户地址为自定义字段 customerArea（LOCATION，存区县 adcode），
     * 按省级 adcode 聚合（LEFT(field_value,2) + '0000'）。仅在时间窗口内统计（startMillis &gt; 0 时生效）。
     */
    List<RegionItem> selectCustomerRegionDist(@Param("orgId") String orgId, @Param("startMillis") long startMillis);

    /**
     * 销售个人核心指标（按负责人）。
     */
    SalesKpi selectSalesKpi(@Param("orgId") String orgId, @Param("ownerId") String ownerId, @Param("monthStart") long monthStart,
                            @Param("startMillis") long startMillis);

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
    List<DistributionItem> selectSalesStageDist(@Param("orgId") String orgId, @Param("ownerId") String ownerId,
                                                @Param("startMillis") long startMillis);

    /**
     * 销售名下客户列表（含合同额、已核销回款）。
     */
    List<CustomerBrief> selectSalesCustomerList(@Param("orgId") String orgId, @Param("ownerId") String ownerId,
                                                @Param("startMillis") long startMillis);

    /**
     * 区域核心指标（按省级 adcode）。
     */
    RegionKpi selectRegionKpi(@Param("orgId") String orgId, @Param("adcode") String adcode,
                              @Param("startMillis") long startMillis);

    /**
     * 区域客户列表（含合同额、已核销回款）。
     */
    List<CustomerBrief> selectRegionCustomerList(@Param("orgId") String orgId, @Param("adcode") String adcode,
                                                 @Param("startMillis") long startMillis);

    /**
     * 区域销售分布（按负责人聚合合同/回款/客户）。
     */
    List<RankItem> selectRegionSalesDist(@Param("orgId") String orgId, @Param("adcode") String adcode,
                                         @Param("startMillis") long startMillis);

    /**
     * 阶段合同列表。
     */
    List<StageContractItem> selectStageContractList(@Param("orgId") String orgId, @Param("stage") String stage,
                                                    @Param("startMillis") long startMillis);

    /**
     * 阶段名称（未配置返回 null，服务层回退展示阶段编码）。
     */
    String selectStageName(@Param("orgId") String orgId, @Param("stage") String stage);

    /**
     * 大客户久未联系：累计合同额达到阈值且最新跟进时间早于给定时间（或从未跟进）的客户。
     */
    List<StaleBigCustomerRow> selectStaleBigCustomer(@Param("orgId") String orgId,
                                                     @Param("amountThreshold") BigDecimal amountThreshold,
                                                     @Param("staleBefore") long staleBefore);

    /**
     * 某人多日无跟进：名下至少有一个非公海客户、但最新跟进记录早于给定时间（或从未跟进）的负责人。
     */
    List<StaleOwnerRow> selectStaleOwner(@Param("orgId") String orgId, @Param("staleBefore") long staleBefore);

    /**
     * 名下至少有一个非公海客户的全部负责人（督办收件人候选）。
     */
    List<String> selectOwnerIds(@Param("orgId") String orgId);
}
