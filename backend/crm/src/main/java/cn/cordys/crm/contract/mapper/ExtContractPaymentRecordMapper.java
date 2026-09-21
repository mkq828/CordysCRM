package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordPageRequest;
import cn.cordys.crm.contract.dto.request.ContractPaymentRecordStatisticRequest;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordResponse;
import cn.cordys.crm.contract.dto.response.ContractPaymentRecordStatisticResponse;
import cn.cordys.crm.contract.dto.response.CustomerPaymentRecordStatisticResponse;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author song-cc-rock
 */
public interface ExtContractPaymentRecordMapper {

    /**
     * 合同回款记录列表
     *
     * @param request            请求参数
     * @param currentUser        当前用户
     * @param currentOrg         当前组织
     * @param deptDataPermission 数据权限
     * @return 回款记录列表
     */
    List<ContractPaymentRecordResponse> list(@Param("request") ContractPaymentRecordPageRequest request, @Param("currentUser") String currentUser,
                                             @Param("currentOrg") String currentOrg, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    /**
     * 通过ID集合获取回款记录
     *
     * @param ids                ID集合
     * @param currentUser        当前用户
     * @param currentOrg         当前组织
     * @param deptDataPermission 数据权限
     * @return 回款记录列表
     */
    List<ContractPaymentRecordResponse> getListByIds(@Param("ids") List<String> ids, @Param("currentUser") String currentUser,
                                                     @Param("currentOrg") String currentOrg, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    /**
     * 汇总客户回款记录金额
     *
     * @param customerId         客户ID
     * @param userId             用户ID
     * @param orgId              组织ID
     * @param deptDataPermission 数据权限
     * @return 汇总结果
     */
    CustomerPaymentRecordStatisticResponse sumCustomerRecordAmount(@Param("customerId") String customerId, @Param("userId") String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    ContractPaymentRecordStatisticResponse searchStatistic(@Param("request") ContractPaymentRecordStatisticRequest request, @Param("orgId") String orgId, @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    void updateRecord(@Param("contractPaymentRecord")ContractPaymentRecord contractPaymentRecord);

    /**
     * 汇总指定回款计划下的已回款金额
     *
     * @param paymentPlanId 回款计划ID
     * @param orgId         组织ID
     * @return 已回款金额合计
     */
    BigDecimal sumRecordAmountByPaymentPlanId(@Param("paymentPlanId") String paymentPlanId, @Param("orgId") String orgId);

    /**
     * 回款核销（待核销 -> 已完成）
     *
     * @param id          回款记录ID
     * @param currentUser 核销人
     * @param verifyTime  核销时间
     * @param remark      核销备注
     * @param proof       收款证明附件ID（逗号分隔）
     */
    void verifyRecord(@Param("id") String id, @Param("currentUser") String currentUser, @Param("verifyTime") Long verifyTime,
                      @Param("remark") String remark, @Param("proof") String proof);

    /**
     * 回款核销撤回（已完成 -> 待核销）
     *
     * @param id          回款记录ID
     * @param currentUser 撤回人
     * @param revokeTime  撤回时间
     * @param remark      撤回备注
     */
    void revokeRecord(@Param("id") String id, @Param("currentUser") String currentUser, @Param("revokeTime") Long revokeTime,
                      @Param("remark") String remark);
}
