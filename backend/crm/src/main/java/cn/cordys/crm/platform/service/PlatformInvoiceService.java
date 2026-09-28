package cn.cordys.crm.platform.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.platform.constants.PlatformInvoiceStatus;
import cn.cordys.crm.platform.domain.PlatformContract;
import cn.cordys.crm.platform.domain.PlatformInvoice;
import cn.cordys.crm.platform.dto.request.PlatformInvoiceActionRequest;
import cn.cordys.crm.platform.dto.request.PlatformInvoicePageRequest;
import cn.cordys.crm.platform.dto.request.PlatformInvoiceSaveRequest;
import cn.cordys.crm.platform.dto.response.PlatformInvoiceResponse;
import cn.cordys.crm.platform.util.PlatformManagerNames;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 平台发票服务：开票 CRUD + 开票/作废；税额 = 开票金额 × 税率
 */
@Service
public class PlatformInvoiceService {

    @Resource
    private BaseMapper<PlatformInvoice> invoiceMapper;
    @Resource
    private BaseMapper<PlatformContract> contractMapper;

    /**
     * 分页查询（内存分页，含合同编号/租户名）
     */
    public Pager<List<PlatformInvoiceResponse>> pageList(PlatformInvoicePageRequest request) {
        List<PlatformInvoice> all = invoiceMapper.selectListByLambda(new LambdaQueryWrapper<PlatformInvoice>()
                .orderByDesc(PlatformInvoice::getCreateTime));
        List<PlatformInvoice> filtered = all.stream()
                .filter(i -> StringUtils.isBlank(request.getContractId()) || request.getContractId().equals(i.getContractId()))
                .filter(i -> StringUtils.isBlank(request.getInvoiceStatus())
                        || request.getInvoiceStatus().equals(i.getInvoiceStatus()))
                .toList();

        List<String> contractIds = filtered.stream()
                .map(PlatformInvoice::getContractId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, PlatformContract> contractMap = contractIds.isEmpty() ? Map.of()
                : contractMapper.selectByIds(new ArrayList<>(contractIds)).stream()
                        .collect(Collectors.toMap(PlatformContract::getId, Function.identity(), (a, b) -> a));

        Map<String, PlatformManagerNames.ManagerNames> managerNames = PlatformManagerNames.resolve(
                filtered.stream().map(PlatformInvoice::getOrganizationId).toList());
        List<PlatformInvoiceResponse> responses = filtered.stream()
                .map(i -> toResponse(i, contractMap, managerNames))
                .toList();
        return paginate(responses, request.getCurrent(), request.getPageSize());
    }

    /**
     * 新增（开票状态=未开，税额自动计算）
     */
    public void add(PlatformInvoiceSaveRequest request, String operatorId) {
        PlatformContract contract = requireContract(request.getContractId());
        long now = System.currentTimeMillis();
        PlatformInvoice invoice = new PlatformInvoice();
        invoice.setId(IDGenerator.nextStr());
        apply(invoice, request);
        invoice.setOrganizationId(contract.getOrganizationId());
        invoice.setInvoiceStatus(PlatformInvoiceStatus.NOT_INVOICED.name());
        invoice.setTaxAmount(calcTax(request.getAmount(), request.getTaxRate()));
        invoice.setCreateTime(now);
        invoice.setUpdateTime(now);
        invoice.setCreateUser(operatorId);
        invoice.setUpdateUser(operatorId);
        invoiceMapper.insert(invoice);
    }

    /**
     * 编辑（重新计算税额，不改变开票状态）
     */
    public void update(PlatformInvoiceSaveRequest request, String operatorId) {
        PlatformInvoice invoice = invoiceMapper.selectByPrimaryKey(request.getId());
        if (invoice == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        apply(invoice, request);
        PlatformContract contract = contractMapper.selectByPrimaryKey(request.getContractId());
        if (contract != null) {
            invoice.setOrganizationId(contract.getOrganizationId());
        }
        invoice.setTaxAmount(calcTax(request.getAmount(), request.getTaxRate()));
        invoice.setUpdateTime(System.currentTimeMillis());
        invoice.setUpdateUser(operatorId);
        invoiceMapper.updateById(invoice);
    }

    /**
     * 删除
     */
    public void delete(String id) {
        if (invoiceMapper.selectByPrimaryKey(id) == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        invoiceMapper.deleteByPrimaryKey(id);
    }

    /**
     * 开票：未开 → 已开，回填发票号
     */
    public void invoice(PlatformInvoiceActionRequest request, String operatorId) {
        PlatformInvoice invoice = invoiceMapper.selectByPrimaryKey(request.getId());
        if (invoice == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        if (!PlatformInvoiceStatus.NOT_INVOICED.name().equals(invoice.getInvoiceStatus())) {
            throw new GenericException(Translator.get("platform.invoice.status.invalid"));
        }
        if (StringUtils.isNotBlank(request.getInvoiceNo())) {
            invoice.setInvoiceNo(request.getInvoiceNo());
        }
        if (StringUtils.isBlank(invoice.getInvoiceNo())) {
            throw new GenericException(Translator.get("platform.invoice.no.required"));
        }
        invoice.setInvoiceStatus(PlatformInvoiceStatus.INVOICED.name());
        invoice.setUpdateTime(System.currentTimeMillis());
        invoice.setUpdateUser(operatorId);
        invoiceMapper.updateById(invoice);
    }

    /**
     * 作废
     */
    public void voidInvoice(PlatformInvoiceActionRequest request, String operatorId) {
        PlatformInvoice invoice = invoiceMapper.selectByPrimaryKey(request.getId());
        if (invoice == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        if (PlatformInvoiceStatus.VOIDED.name().equals(invoice.getInvoiceStatus())) {
            return;
        }
        if (!PlatformInvoiceStatus.INVOICED.name().equals(invoice.getInvoiceStatus())) {
            throw new GenericException(Translator.get("platform.invoice.status.invalid"));
        }
        invoice.setInvoiceStatus(PlatformInvoiceStatus.VOIDED.name());
        invoice.setUpdateTime(System.currentTimeMillis());
        invoice.setUpdateUser(operatorId);
        invoiceMapper.updateById(invoice);
    }

    private PlatformContract requireContract(String contractId) {
        PlatformContract contract = contractMapper.selectByPrimaryKey(contractId);
        if (contract == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        return contract;
    }

    private void apply(PlatformInvoice invoice, PlatformInvoiceSaveRequest request) {
        invoice.setContractId(request.getContractId());
        invoice.setInvoiceNo(request.getInvoiceNo());
        invoice.setInvoiceType(request.getInvoiceType());
        invoice.setAmount(request.getAmount());
        invoice.setTaxRate(request.getTaxRate() == null ? BigDecimal.valueOf(6) : request.getTaxRate());
        invoice.setBusinessTitle(request.getBusinessTitle());
        invoice.setRemark(request.getRemark());
    }

    private BigDecimal calcTax(BigDecimal amount, BigDecimal taxRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = taxRate == null ? BigDecimal.valueOf(6) : taxRate;
        return amount.multiply(rate).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private PlatformInvoiceResponse toResponse(PlatformInvoice invoice,
                                               Map<String, PlatformContract> contractMap,
                                               Map<String, PlatformManagerNames.ManagerNames> managerNames) {
        PlatformInvoiceResponse response = new PlatformInvoiceResponse();
        response.setId(invoice.getId());
        response.setContractId(invoice.getContractId());
        response.setOrganizationId(invoice.getOrganizationId());
        response.setInvoiceNo(invoice.getInvoiceNo());
        response.setInvoiceType(invoice.getInvoiceType());
        response.setAmount(invoice.getAmount());
        response.setTaxRate(invoice.getTaxRate());
        response.setTaxAmount(invoice.getTaxAmount());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setBusinessTitle(invoice.getBusinessTitle());
        response.setRemark(invoice.getRemark());
        response.setCreateTime(invoice.getCreateTime());
        response.setCreateUser(invoice.getCreateUser());
        response.setUpdateTime(invoice.getUpdateTime());
        response.setUpdateUser(invoice.getUpdateUser());
        PlatformContract contract = contractMap.get(invoice.getContractId());
        if (contract != null) {
            response.setContractNo(contract.getContractNo());
            response.setOrgName(contract.getOrgName());
        }
        PlatformManagerNames.ManagerNames mn = managerNames.get(invoice.getOrganizationId());
        response.setSignManagerName(mn == null ? null : mn.signManagerName());
        response.setFollowManagerName(mn == null ? null : mn.followManagerName());
        return response;
    }

    private <T> Pager<List<T>> paginate(List<T> list, int current, int pageSize) {
        int page = current <= 0 ? 1 : current;
        int size = pageSize <= 0 ? 20 : pageSize;
        int total = list.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        Pager<List<T>> pager = new Pager<>();
        pager.setList(list.subList(from, to));
        pager.setTotal(total);
        pager.setPageSize(size);
        pager.setCurrent(page);
        return pager;
    }
}
