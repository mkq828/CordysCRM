package cn.cordys.crm.ai.employee.action.mapper;

import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
import cn.cordys.crm.ai.employee.action.dto.ClueFlowRow;
import cn.cordys.crm.ai.employee.action.dto.DunningRow;
import cn.cordys.crm.ai.employee.action.dto.FillFieldCustomerRow;
import cn.cordys.crm.ai.employee.action.dto.FillFieldValueRow;
import cn.cordys.crm.ai.employee.action.dto.StaleCustomerRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 员工待确认动作的扩展查询。均以 organization_id 兜底隔离。
 */
public interface ExtAiEmployeeActionMapper {

    /**
     * 「久未联系」客户：非公海、有负责人、且最新跟进时间早于给定时间（或从未跟进）。
     * 按累计合同额降序、跟进时间升序排列，取前 limit 条。
     */
    List<StaleCustomerRow> selectStaleCustomers(@Param("orgId") String orgId,
                                                @Param("staleBefore") long staleBefore,
                                                @Param("limit") int limit);

    /**
     * 当前租户所有 PENDING 待确认动作（用于去重与列表）。
     */
    List<AiEmployeeAction> selectPending(@Param("orgId") String orgId);

    /**
     * 驳回静默期内的动作：最近被驳回、且距驳回未超过静默天数的动作，扫描时按 actionType+resourceId 去重跳过，
     * 避免对同一资源天天重复打扰。
     */
    List<AiEmployeeAction> selectRejectedRecent(@Param("orgId") String orgId, @Param("quietBefore") long quietBefore);

    /**
     * 催款候选：到期时间落在 [lower, upper) 窗口内的未终止合同，以及计划状态为 PENDING/PARTIALLY_COMPLETED 的回款计划，
     * 合并按到期时间升序取前 limit 条。
     */
    List<DunningRow> selectDunningCandidates(@Param("orgId") String orgId,
                                             @Param("lower") long lower,
                                             @Param("upper") long upper,
                                             @Param("limit") int limit);

    /**
     * 打标签候选：久未联系（最新跟进早于 churnBefore 或从未跟进）且尚未打指定名称标签的客户。
     */
    List<StaleCustomerRow> selectChurnCustomers(@Param("orgId") String orgId,
                                                @Param("churnBefore") long churnBefore,
                                                @Param("churnTagName") String churnTagName,
                                                @Param("limit") int limit);

    /**
     * 线索流转候选：「感兴趣」阶段、近 followAfter 以来有跟进、尚未转客户/商机的线索。
     */
    List<ClueFlowRow> selectClueFlowCandidates(@Param("orgId") String orgId,
                                               @Param("followAfter") long followAfter,
                                               @Param("limit") int limit);

    /**
     * 补全数据候选：必填文本字段（fieldIds）未全部填写的客户（已填字段数 &lt; requiredCount）。
     */
    List<FillFieldCustomerRow> selectFillFieldCandidates(@Param("orgId") String orgId,
                                                         @Param("fieldIds") List<String> fieldIds,
                                                         @Param("requiredCount") int requiredCount,
                                                         @Param("limit") int limit);

    /**
     * 补全数据候选客户中「已填写的必填文本字段」，供扫描服务反推缺失字段。
     */
    List<FillFieldValueRow> selectExistingFieldIds(@Param("orgId") String orgId,
                                                   @Param("customerIds") List<String> customerIds,
                                                   @Param("fieldIds") List<String> fieldIds);
}
