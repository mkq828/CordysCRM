package cn.cordys.crm.finance.mapper;

import cn.cordys.crm.finance.dto.FinanceContractRow;
import cn.cordys.crm.finance.dto.response.FinancePaymentRecordResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务应收聚合查询
 */
public interface ExtFinanceMapper {

    /**
     * 查询组织下所有合同及其客户、已核销回款金额
     *
     * @param orgId   组织ID
     * @param keyword 关键词（客户名称/合同名称/合同编号）
     * @return 合同聚合行
     */
    List<FinanceContractRow> listContracts(@Param("orgId") String orgId, @Param("keyword") String keyword);

    /**
     * 查询指定合同下的回款明细（含核销信息）
     *
     * @param orgId       组织ID
     * @param contractIds 合同ID集合
     * @return 回款明细
     */
    List<FinancePaymentRecordResponse> listPaymentRecords(@Param("orgId") String orgId, @Param("contractIds") List<String> contractIds);
}
