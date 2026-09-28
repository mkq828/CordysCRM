package cn.cordys.crm.platform.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.platform.constants.PlatformPaymentVerificationStatus;
import cn.cordys.crm.platform.domain.PlatformContract;
import cn.cordys.crm.platform.domain.PlatformPaymentRecord;
import cn.cordys.crm.platform.dto.request.PlatformPaymentRecordPageRequest;
import cn.cordys.crm.platform.dto.request.PlatformPaymentRecordSaveRequest;
import cn.cordys.crm.platform.dto.request.PlatformRevokeRequest;
import cn.cordys.crm.platform.dto.request.PlatformVerifyRequest;
import cn.cordys.crm.platform.dto.response.PlatformAttachmentResponse;
import cn.cordys.crm.platform.dto.response.PlatformPaymentRecordResponse;
import cn.cordys.crm.platform.util.PlatformManagerNames;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.TenantPlanService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 平台回款服务：回款 CRUD + 核销/撤回（复刻租户层 contract_payment_record 语义），首次核销自动开通企业版
 */
@Service
public class PlatformPaymentRecordService {

    @Resource
    private BaseMapper<PlatformPaymentRecord> recordMapper;
    @Resource
    private BaseMapper<PlatformContract> contractMapper;
    @Resource
    private BaseMapper<Organization> organizationMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private TenantPlanService tenantPlanService;
    @Resource
    private AttachmentService attachmentService;

    /**
     * 分页查询（内存分页，含合同编号/租户名/付款凭证/收款证明）
     */
    public Pager<List<PlatformPaymentRecordResponse>> pageList(PlatformPaymentRecordPageRequest request) {
        Set<String> scope = scopedOrgIds();
        List<PlatformPaymentRecord> all = recordMapper.selectListByLambda(new LambdaQueryWrapper<PlatformPaymentRecord>()
                .orderByDesc(PlatformPaymentRecord::getCreateTime));
        List<PlatformPaymentRecord> filtered = all.stream()
                .filter(r -> scope == null || scope.contains(r.getOrganizationId()))
                .filter(r -> StringUtils.isBlank(request.getContractId()) || request.getContractId().equals(r.getContractId()))
                .filter(r -> StringUtils.isBlank(request.getVerificationStatus())
                        || request.getVerificationStatus().equals(r.getVerificationStatus()))
                .toList();

        List<String> contractIds = filtered.stream()
                .map(PlatformPaymentRecord::getContractId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, PlatformContract> contractMap = contractIds.isEmpty() ? Map.of()
                : contractMapper.selectByIds(new ArrayList<>(contractIds)).stream()
                        .collect(Collectors.toMap(PlatformContract::getId, Function.identity(), (a, b) -> a));

        Set<String> attachmentIds = new HashSet<>();
        filtered.forEach(r -> {
            collectIds(r.getVoucherAttachmentIds(), attachmentIds);
            collectIds(r.getVerifyProof(), attachmentIds);
        });
        Map<String, Attachment> attachmentMap = attachmentIds.isEmpty() ? Map.of()
                : attachmentMapper.selectByIds(new ArrayList<>(attachmentIds)).stream()
                        .collect(Collectors.toMap(Attachment::getId, Function.identity(), (a, b) -> a));

        Map<String, PlatformManagerNames.ManagerNames> managerNames = PlatformManagerNames.resolve(
                filtered.stream().map(PlatformPaymentRecord::getOrganizationId).toList());
        List<PlatformPaymentRecordResponse> responses = filtered.stream()
                .map(r -> toResponse(r, contractMap, attachmentMap, managerNames))
                .toList();
        return paginate(responses, request.getCurrent(), request.getPageSize());
    }

    /**
     * 新增（核销状态=待核销，租户取合同的 organization_id）
     */
    public void add(PlatformPaymentRecordSaveRequest request, String operatorId) {
        PlatformContract contract = requireContract(request.getContractId());
        checkOrgInScope(contract.getOrganizationId(), scopedOrgIds());
        // 一次性回款：同一合同只允许一条回款记录（待核销/已完成/已撤回都算），删除后才能重录
        boolean exists = !recordMapper.selectListByLambda(new LambdaQueryWrapper<PlatformPaymentRecord>()
                .eq(PlatformPaymentRecord::getContractId, request.getContractId())).isEmpty();
        if (exists) {
            throw new GenericException(Translator.get("platform.contract.already.paid"));
        }
        long now = System.currentTimeMillis();
        PlatformPaymentRecord record = new PlatformPaymentRecord();
        record.setId(IDGenerator.nextStr());
        apply(record, request);
        // 一次性全额：回款金额固定 = 合同金额，不信任前端
        record.setAmount(contract.getAmount());
        record.setOrganizationId(contract.getOrganizationId());
        record.setVerificationStatus(PlatformPaymentVerificationStatus.PENDING.name());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setCreateUser(operatorId);
        record.setUpdateUser(operatorId);
        recordMapper.insert(record);
        // 付款凭证转存为正式附件，否则列表/编辑回显不到
        syncAttachments(record, operatorId);
    }

    /**
     * 编辑
     */
    public void update(PlatformPaymentRecordSaveRequest request, String operatorId) {
        PlatformPaymentRecord record = recordMapper.selectByPrimaryKey(request.getId());
        if (record == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        checkVerifiedLocked(record);
        Set<String> scope = scopedOrgIds();
        checkOrgInScope(record.getOrganizationId(), scope);
        PlatformContract contract = requireContract(request.getContractId());
        checkOrgInScope(contract.getOrganizationId(), scope);
        // 一次性回款唯一性：改挂到别的合同时，该合同不得已有回款记录
        boolean contractChanged = !request.getContractId().equals(record.getContractId());
        if (contractChanged) {
            boolean exists = !recordMapper.selectListByLambda(new LambdaQueryWrapper<PlatformPaymentRecord>()
                    .eq(PlatformPaymentRecord::getContractId, request.getContractId())).isEmpty();
            if (exists) {
                throw new GenericException(Translator.get("platform.contract.already.paid"));
            }
        }
        apply(record, request);
        // 一次性全额：回款金额固定 = 合同金额，不信任前端
        record.setAmount(contract.getAmount());
        record.setOrganizationId(contract.getOrganizationId());
        record.setUpdateTime(System.currentTimeMillis());
        record.setUpdateUser(operatorId);
        recordMapper.updateById(record);
        // 编辑后同步凭证附件（新增转存、移除删除）
        syncAttachments(record, operatorId);
    }

    /**
     * 删除
     */
    public void delete(String id, String operatorId) {
        PlatformPaymentRecord record = recordMapper.selectByPrimaryKey(id);
        if (record == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        checkVerifiedLocked(record);
        checkOrgInScope(record.getOrganizationId(), scopedOrgIds());
        recordMapper.deleteByPrimaryKey(id);
        // 同步清空付款凭证 + 收款证明附件（空列表 = 删 sys_attachment 行 + 删文件）
        attachmentService.processTemp(new UploadTransferRequest(
                record.getOrganizationId(), record.getId(), operatorId, List.of()));
    }

    /**
     * 回款核销：待核销 → 已完成；首笔核销自动开通企业版（写版本快照 + 生效/到期时间）
     */
    public void verify(PlatformVerifyRequest request, String operatorId) {
        PlatformPaymentRecord record = recordMapper.selectByPrimaryKey(request.getId());
        if (record == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        if (PlatformPaymentVerificationStatus.DONE.name().equals(record.getVerificationStatus())) {
            return;
        }
        // 首次核销判定：该合同当前无 DONE 记录，且本记录从未进入过 DONE（撤回会置 revokeTime）。
        // 撤回只做记账回退、不回滚已开通企业版，故「撤回→再核销」不得重复顺延有效期。
        boolean firstVerify = !hasDoneRecord(record.getContractId()) && record.getRevokeTime() == null;
        String proof = CollectionUtils.isEmpty(request.getProofAttachmentIds())
                ? null : String.join(",", request.getProofAttachmentIds());
        long now = System.currentTimeMillis();
        record.setVerificationStatus(PlatformPaymentVerificationStatus.DONE.name());
        record.setVerifyUser(operatorId);
        record.setVerifyTime(now);
        record.setVerifyRemark(request.getRemark());
        record.setVerifyProof(proof);
        record.setRevokeUser(null);
        record.setRevokeTime(null);
        record.setRevokeRemark(null);
        record.setUpdateTime(now);
        record.setUpdateUser(operatorId);
        recordMapper.updateById(record);
        // 收款证明转存为正式附件
        syncAttachments(record, operatorId);

        if (firstVerify) {
            PlatformContract contract = contractMapper.selectByPrimaryKey(record.getContractId());
            if (contract != null && StringUtils.isNotBlank(contract.getEditionCode())) {
                tenantPlanService.activateByOrganization(contract.getOrganizationId(),
                        contract.getEditionCode(), contract.getValidityDays(), operatorId);
            }
        }
    }

    /**
     * 撤回核销：已完成 → 待核销（仅记账回退，不回滚已开通的企业版）
     */
    public void revoke(PlatformRevokeRequest request, String operatorId) {
        PlatformPaymentRecord record = recordMapper.selectByPrimaryKey(request.getId());
        if (record == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        if (PlatformPaymentVerificationStatus.PENDING.name().equals(record.getVerificationStatus())) {
            return;
        }
        long now = System.currentTimeMillis();
        record.setVerificationStatus(PlatformPaymentVerificationStatus.PENDING.name());
        record.setRevokeUser(operatorId);
        record.setRevokeTime(now);
        record.setRevokeRemark(request.getRemark());
        record.setVerifyUser(null);
        record.setVerifyTime(null);
        record.setVerifyRemark(null);
        record.setVerifyProof(null);
        record.setUpdateTime(now);
        record.setUpdateUser(operatorId);
        recordMapper.updateById(record);
        // 撤回后清掉收款证明附件，保留付款凭证
        syncAttachments(record, operatorId);
    }

    /**
     * 数据范围：admin 返回 null（不限）；城市经理返回本人归属（签约/跟进）租户 ID 集合。
     */
    private Set<String> scopedOrgIds() {
        if (!PlatformSessionUtils.isCityManager()) {
            return null;
        }
        String userId = SessionUtils.getUserId();
        return DataAccessLayer.with(Organization.class)
                .selectListByLambda(new LambdaQueryWrapper<Organization>()).stream()
                .filter(o -> userId.equals(o.getSignManagerId()) || userId.equals(o.getFollowManagerId()))
                .map(Organization::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 校验租户是否在当前用户数据范围内（城市经理只能操作自己归属租户的回款）。
     */
    private void checkOrgInScope(String organizationId, Set<String> scope) {
        if (scope != null && (StringUtils.isBlank(organizationId) || !scope.contains(organizationId))) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    /**
     * 已核销（DONE）的回款锁定：不可编辑/删除，需先由 admin 撤回核销。
     */
    private void checkVerifiedLocked(PlatformPaymentRecord record) {
        if (PlatformPaymentVerificationStatus.DONE.name().equals(record.getVerificationStatus())) {
            throw new GenericException(Translator.get("platform.payment.verified.locked"));
        }
    }

    private PlatformContract requireContract(String contractId) {
        PlatformContract contract = contractMapper.selectByPrimaryKey(contractId);
        if (contract == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        return contract;
    }

    private boolean hasDoneRecord(String contractId) {
        return !recordMapper.selectListByLambda(new LambdaQueryWrapper<PlatformPaymentRecord>()
                .eq(PlatformPaymentRecord::getContractId, contractId)
                .eq(PlatformPaymentRecord::getVerificationStatus, PlatformPaymentVerificationStatus.DONE.name()))
                .isEmpty();
    }

    private void apply(PlatformPaymentRecord record, PlatformPaymentRecordSaveRequest request) {
        record.setContractId(request.getContractId());
        record.setRecordNo(request.getRecordNo());
        record.setAmount(request.getAmount());
        record.setPaymentType(request.getPaymentType());
        record.setBankAccount(request.getBankAccount());
        record.setVoucherAttachmentIds(request.getVoucherAttachmentIds());
        record.setRemark(request.getRemark());
    }

    private PlatformPaymentRecordResponse toResponse(PlatformPaymentRecord record,
                                                     Map<String, PlatformContract> contractMap,
                                                     Map<String, Attachment> attachmentMap,
                                                     Map<String, PlatformManagerNames.ManagerNames> managerNames) {
        PlatformPaymentRecordResponse response = new PlatformPaymentRecordResponse();
        response.setId(record.getId());
        response.setContractId(record.getContractId());
        response.setOrganizationId(record.getOrganizationId());
        response.setRecordNo(record.getRecordNo());
        response.setAmount(record.getAmount());
        response.setPaymentType(record.getPaymentType());
        response.setBankAccount(record.getBankAccount());
        response.setVoucherAttachmentIds(record.getVoucherAttachmentIds());
        response.setVerificationStatus(record.getVerificationStatus());
        response.setVerifyUser(record.getVerifyUser());
        response.setVerifyTime(record.getVerifyTime());
        response.setVerifyRemark(record.getVerifyRemark());
        response.setVerifyProof(record.getVerifyProof());
        response.setRevokeUser(record.getRevokeUser());
        response.setRevokeTime(record.getRevokeTime());
        response.setRevokeRemark(record.getRevokeRemark());
        response.setRemark(record.getRemark());
        response.setCreateTime(record.getCreateTime());
        response.setCreateUser(record.getCreateUser());
        PlatformContract contract = contractMap.get(record.getContractId());
        if (contract != null) {
            response.setContractNo(contract.getContractNo());
            response.setOrgName(contract.getOrgName());
        }
        response.setVoucherList(toAttachments(record.getVoucherAttachmentIds(), attachmentMap));
        response.setProofList(toAttachments(record.getVerifyProof(), attachmentMap));
        PlatformManagerNames.ManagerNames mn = managerNames.get(record.getOrganizationId());
        response.setSignManagerName(mn == null ? null : mn.signManagerName());
        response.setFollowManagerName(mn == null ? null : mn.followManagerName());
        return response;
    }

    private List<PlatformAttachmentResponse> toAttachments(String idStr, Map<String, Attachment> attachmentMap) {
        if (StringUtils.isBlank(idStr)) {
            return List.of();
        }
        return Arrays.stream(idStr.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(attachmentMap::get)
                .filter(java.util.Objects::nonNull)
                .map(a -> {
                    PlatformAttachmentResponse response = new PlatformAttachmentResponse();
                    response.setId(a.getId());
                    response.setName(a.getName());
                    response.setType(a.getType());
                    response.setSize(a.getSize());
                    return response;
                })
                .toList();
    }

    private void collectIds(String idStr, Set<String> target) {
        if (StringUtils.isBlank(idStr)) {
            return;
        }
        Arrays.stream(idStr.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .forEach(target::add);
    }

    /**
     * 将凭证 + 收款证明的临时附件 ID 一并转存为正式附件（写 sys_attachment）。
     * 以 record.id 为 resourceId 做「集合同步」：临时 ID 不在库里的转存新增，库里多余的删除，
     * 因此撤回（证明清空）会删证明、保留凭证，核销会新增证明、保留凭证。
     */
    private void syncAttachments(PlatformPaymentRecord record, String operatorId) {
        Set<String> ids = new HashSet<>();
        collectIds(record.getVoucherAttachmentIds(), ids);
        collectIds(record.getVerifyProof(), ids);
        attachmentService.processTemp(new UploadTransferRequest(
                record.getOrganizationId(), record.getId(), operatorId, new ArrayList<>(ids)));
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
