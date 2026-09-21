package cn.cordys.crm.finance.service;

import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.crm.contract.constants.ContractPaymentVerificationStatus;
import cn.cordys.crm.finance.dto.FinanceContractRow;
import cn.cordys.crm.finance.dto.request.FinancePageRequest;
import cn.cordys.crm.finance.dto.response.FinanceContractResponse;
import cn.cordys.crm.finance.dto.response.FinanceCustomerGroupResponse;
import cn.cordys.crm.finance.dto.response.FinanceOverviewResponse;
import cn.cordys.crm.finance.dto.response.FinancePaymentRecordResponse;
import cn.cordys.crm.finance.mapper.ExtFinanceMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务应收（AR）服务：实时统计 + 客户/合同两级应收视图 + 回款核销。
 */
@Service
@Slf4j
public class FinanceService {

    @Resource
    private ExtFinanceMapper extFinanceMapper;
    @Resource
    private BaseService baseService;

    /**
     * 应收概览统计
     */
    public FinanceOverviewResponse overview(String orgId) {
        List<FinanceContractRow> contracts = extFinanceMapper.listContracts(orgId, null);
        FinanceOverviewResponse response = new FinanceOverviewResponse();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal verifiedAmount = BigDecimal.ZERO;
        BigDecimal unverifiedAmount = BigDecimal.ZERO;
        long unsettledContractCount = 0;
        Set<String> customerIds = new HashSet<>();

        for (FinanceContractRow row : contracts) {
            BigDecimal amount = zeroIfNull(row.getAmount());
            BigDecimal verified = zeroIfNull(row.getVerifiedAmount());
            totalAmount = totalAmount.add(amount);
            verifiedAmount = verifiedAmount.add(verified);
            if (StringUtils.isNotBlank(row.getCustomerId())) {
                customerIds.add(row.getCustomerId());
            }
            if (amount.compareTo(verified) > 0) {
                unsettledContractCount++;
            }
        }

        if (CollectionUtils.isNotEmpty(contracts)) {
            List<String> contractIds = contracts.stream().map(FinanceContractRow::getContractId).toList();
            for (FinancePaymentRecordResponse record : extFinanceMapper.listPaymentRecords(orgId, contractIds)) {
                if (ContractPaymentVerificationStatus.PENDING.name().equals(record.getVerificationStatus())) {
                    unverifiedAmount = unverifiedAmount.add(zeroIfNull(record.getRecordAmount()));
                }
            }
        }

        response.setTotalContractAmount(totalAmount);
        response.setVerifiedAmount(verifiedAmount);
        response.setPendingAmount(totalAmount.subtract(verifiedAmount));
        response.setUnverifiedAmount(unverifiedAmount);
        response.setCustomerCount(customerIds.size());
        response.setContractCount(contracts.size());
        response.setUnsettledContractCount(unsettledContractCount);
        return response;
    }

    /**
     * 应收列表：客户分组（有尾款客户在前，创建时间倒序），组内合同，合同下回款明细。
     */
    public PagerWithOption<List<FinanceCustomerGroupResponse>> page(FinancePageRequest request, String orgId) {
        List<FinanceContractRow> contracts = extFinanceMapper.listContracts(orgId, request.getKeyword());
        Map<String, List<FinancePaymentRecordResponse>> recordMap = loadRecordMap(orgId, contracts);

        // 合同 -> 客户分组
        Map<String, FinanceCustomerGroupResponse> customerMap = new LinkedHashMap<>();
        for (FinanceContractRow row : contracts) {
            FinanceCustomerGroupResponse customer = customerMap.computeIfAbsent(row.getCustomerId(), id -> {
                FinanceCustomerGroupResponse group = new FinanceCustomerGroupResponse();
                group.setCustomerId(id);
                group.setCustomerName(row.getCustomerName());
                group.setCustomerCreateTime(row.getCustomerCreateTime());
                group.setTotalAmount(BigDecimal.ZERO);
                group.setVerifiedAmount(BigDecimal.ZERO);
                group.setPendingAmount(BigDecimal.ZERO);
                group.setContracts(new ArrayList<>());
                return group;
            });

            BigDecimal amount = zeroIfNull(row.getAmount());
            BigDecimal verified = zeroIfNull(row.getVerifiedAmount());
            BigDecimal pending = amount.subtract(verified);

            customer.setTotalAmount(customer.getTotalAmount().add(amount));
            customer.setVerifiedAmount(customer.getVerifiedAmount().add(verified));
            customer.setPendingAmount(customer.getPendingAmount().add(pending));

            FinanceContractResponse contract = new FinanceContractResponse();
            contract.setContractId(row.getContractId());
            contract.setContractName(row.getContractName());
            contract.setContractNumber(row.getContractNumber());
            contract.setAmount(amount);
            contract.setVerifiedAmount(verified);
            contract.setPendingAmount(pending);
            contract.setSettled(pending.compareTo(BigDecimal.ZERO) <= 0);
            contract.setCreateTime(row.getCreateTime());
            contract.setRecords(recordMap.getOrDefault(row.getContractId(), Collections.emptyList()));
            customer.getContracts().add(contract);
        }

        List<FinanceCustomerGroupResponse> customers = new ArrayList<>(customerMap.values());
        for (FinanceCustomerGroupResponse customer : customers) {
            customer.setSettled(customer.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0);
        }

        // 排序：未结清在前，结清在后；组内按客户创建时间倒序
        customers.sort(Comparator
                .comparing(FinanceCustomerGroupResponse::isSettled)
                .thenComparing(FinanceCustomerGroupResponse::getCustomerCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        // 内存分页
        int current = request.getCurrent() <= 0 ? 1 : request.getCurrent();
        int pageSize = request.getPageSize() <= 0 ? 20 : request.getPageSize();
        int total = customers.size();
        int fromIndex = Math.min((current - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<FinanceCustomerGroupResponse> pageList = customers.subList(fromIndex, toIndex);

        PagerWithOption<List<FinanceCustomerGroupResponse>> pager = new PagerWithOption<>();
        pager.setList(pageList);
        pager.setTotal(total);
        pager.setPageSize(pageSize);
        pager.setCurrent(current);
        return pager;
    }

    private Map<String, List<FinancePaymentRecordResponse>> loadRecordMap(String orgId, List<FinanceContractRow> contracts) {
        if (CollectionUtils.isEmpty(contracts)) {
            return Collections.emptyMap();
        }
        List<String> contractIds = contracts.stream().map(FinanceContractRow::getContractId).filter(StringUtils::isNotBlank).toList();
        List<FinancePaymentRecordResponse> records = contractIds.isEmpty()
                ? Collections.emptyList()
                : extFinanceMapper.listPaymentRecords(orgId, contractIds);

        // 核销人/撤回人名称
        Set<String> userIds = new HashSet<>();
        records.forEach(record -> {
            if (StringUtils.isNotBlank(record.getVerifyUser())) {
                userIds.add(record.getVerifyUser());
            }
            if (StringUtils.isNotBlank(record.getRevokeUser())) {
                userIds.add(record.getRevokeUser());
            }
        });
        Map<String, String> userNameMap = userIds.isEmpty() ? Collections.emptyMap() : baseService.getUserNameMap(new ArrayList<>(userIds));
        records.forEach(record -> {
            if (StringUtils.isNotBlank(record.getVerifyUser())) {
                record.setVerifyUserName(baseService.getAndCheckOptionName(userNameMap.get(record.getVerifyUser())));
            }
            if (StringUtils.isNotBlank(record.getRevokeUser())) {
                record.setRevokeUserName(baseService.getAndCheckOptionName(userNameMap.get(record.getRevokeUser())));
            }
        });

        return records.stream()
                .filter(record -> StringUtils.isNotBlank(record.getContractId()))
                .collect(Collectors.groupingBy(FinancePaymentRecordResponse::getContractId));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
