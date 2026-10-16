package cn.cordys.crm.ai.employee.action.mapper;

import cn.cordys.crm.ai.employee.action.domain.AiEmployeeAction;
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
     * 驳回静默期内的客户：最近被驳回、且距驳回未超过静默天数的客户，扫描时跳过，避免天天重复打扰。
     */
    List<String> selectQuietCustomers(@Param("orgId") String orgId, @Param("quietBefore") long quietBefore);
}
