package cn.cordys.crm.platform.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.platform.constants.PlatformContractStatus;
import cn.cordys.crm.platform.domain.PlatformContract;
import cn.cordys.crm.platform.dto.request.PlatformContractPageRequest;
import cn.cordys.crm.platform.dto.request.PlatformContractSaveRequest;
import cn.cordys.crm.platform.dto.request.PlatformContractStatusRequest;
import cn.cordys.crm.platform.dto.response.PlatformAttachmentResponse;
import cn.cordys.crm.platform.dto.response.PlatformContractOptionResponse;
import cn.cordys.crm.platform.dto.response.PlatformContractResponse;
import cn.cordys.crm.platform.dto.response.PlatformOrgOptionResponse;
import cn.cordys.crm.platform.util.PlatformManagerNames;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.SysEdition;
import cn.cordys.crm.system.domain.TenantPlan;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.crm.system.service.TenantPlanService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.DataAccessLayer;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 平台合同服务：合同 CRUD + 状态机（草稿→待签署→已完成→已归档/已作废）
 */
@Service
public class PlatformContractService {

    @Resource
    private BaseMapper<PlatformContract> contractMapper;
    @Resource
    private BaseMapper<Organization> organizationMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private EditionService editionService;
    @Resource
    private TenantPlanService tenantPlanService;
    @Resource
    private AttachmentService attachmentService;

    /**
     * 分页查询（内存分页，关键字匹配合同编号/租户名称；含已解析的扫描件列表，供列表直接查看/预览附件）
     */
    public Pager<List<PlatformContractResponse>> pageList(PlatformContractPageRequest request) {
        Set<String> scope = scopedOrgIds();
        List<PlatformContract> all = contractMapper.selectListByLambda(new LambdaQueryWrapper<PlatformContract>()
                .orderByDesc(PlatformContract::getCreateTime));
        List<PlatformContract> filtered = all.stream()
                .filter(c -> scope == null || scope.contains(c.getOrganizationId()))
                .filter(c -> StringUtils.isBlank(request.getStatus()) || request.getStatus().equals(c.getStatus()))
                .filter(c -> StringUtils.isBlank(request.getKeyword())
                        || (c.getContractNo() != null && c.getContractNo().contains(request.getKeyword()))
                        || (c.getOrgName() != null && c.getOrgName().contains(request.getKeyword())))
                .toList();
        Pager<List<PlatformContract>> paged = paginate(filtered, request.getCurrent(), request.getPageSize());
        Map<String, PlatformManagerNames.ManagerNames> managerNames = PlatformManagerNames.resolve(
                paged.getList().stream().map(PlatformContract::getOrganizationId).toList());
        List<PlatformContractResponse> responses = paged.getList().stream()
                .map(c -> {
                    PlatformContractResponse response = new PlatformContractResponse();
                    copy(c, response);
                    PlatformManagerNames.ManagerNames mn = managerNames.get(c.getOrganizationId());
                    response.setSignManagerName(mn == null ? null : mn.signManagerName());
                    response.setFollowManagerName(mn == null ? null : mn.followManagerName());
                    response.setAttachmentList(resolveAttachments(c.getAttachmentIds()));
                    return response;
                })
                .toList();
        Pager<List<PlatformContractResponse>> result = new Pager<>();
        result.setList(responses);
        result.setTotal(paged.getTotal());
        result.setPageSize(paged.getPageSize());
        result.setCurrent(paged.getCurrent());
        return result;
    }

    /**
     * 详情（含已解析的扫描件列表）
     */
    public PlatformContractResponse get(String id) {
        PlatformContract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        checkOrgInScope(contract.getOrganizationId(), scopedOrgIds());
        PlatformContractResponse response = new PlatformContractResponse();
        copy(contract, response);
        response.setAttachmentList(resolveAttachments(contract.getAttachmentIds()));
        return response;
    }

    /**
     * 新增（状态=草稿，回填租户主体/版本快照）
     */
    public void add(PlatformContractSaveRequest request, String operatorId) {
        checkOrgInScope(request.getOrganizationId(), scopedOrgIds());
        long now = System.currentTimeMillis();
        PlatformContract contract = new PlatformContract();
        contract.setId(IDGenerator.nextStr());
        apply(contract, request);
        assignSignManagerToOrg(request, operatorId);
        contract.setStatus(PlatformContractStatus.DRAFT.name());
        contract.setCreateTime(now);
        contract.setUpdateTime(now);
        contract.setCreateUser(operatorId);
        contract.setUpdateUser(operatorId);
        contractMapper.insert(contract);
        // 扫描件转存为正式附件（临时附件 → sys_attachment），否则列表/编辑回显不到
        syncAttachments(contract.getOrganizationId(), contract.getId(), operatorId, contract.getAttachmentIds());
    }

    /**
     * 编辑（重新回填快照）
     */
    public void update(PlatformContractSaveRequest request, String operatorId) {
        PlatformContract contract = contractMapper.selectByPrimaryKey(request.getId());
        if (contract == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        Set<String> scope = scopedOrgIds();
        checkOrgInScope(contract.getOrganizationId(), scope);
        checkOrgInScope(request.getOrganizationId(), scope);
        apply(contract, request);
        assignSignManagerToOrg(request, operatorId);
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(operatorId);
        contractMapper.updateById(contract);
        // 编辑后按最新扫描件同步（新增转存、移除删除），空列表则清空
        syncAttachments(contract.getOrganizationId(), contract.getId(), operatorId, contract.getAttachmentIds());
    }

    /**
     * 删除
     */
    public void delete(String id, String operatorId) {
        PlatformContract contract = contractMapper.selectByPrimaryKey(id);
        if (contract == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        checkOrgInScope(contract.getOrganizationId(), scopedOrgIds());
        contractMapper.deleteByPrimaryKey(id);
        // 同步清空该合同的扫描件附件（删 sys_attachment 行 + 删文件）
        syncAttachments(contract.getOrganizationId(), contract.getId(), operatorId, null);
    }

    /**
     * 状态流转（目标状态必须是合法枚举）
     */
    public void changeStatus(PlatformContractStatusRequest request, String operatorId) {
        PlatformContract contract = contractMapper.selectByPrimaryKey(request.getId());
        if (contract == null) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
        checkOrgInScope(contract.getOrganizationId(), scopedOrgIds());
        PlatformContractStatus target = parseStatus(request.getStatus());
        contract.setStatus(target.name());
        contract.setUpdateTime(System.currentTimeMillis());
        contract.setUpdateUser(operatorId);
        contractMapper.updateById(contract);
    }

    /**
     * 租户（组织）下拉选项（平台侧，排除默认组织）
     */
    public List<PlatformOrgOptionResponse> listOrgOptions() {
        Set<String> scope = scopedOrgIds();
        // 已有有效合同（草稿/作废不计）的租户：前端据此判断「首份合同用首年价、续约用年价」
        Set<String> orgsWithContract = contractMapper.selectListByLambda(new LambdaQueryWrapper<PlatformContract>()).stream()
                .filter(c -> !PlatformContractStatus.DRAFT.name().equals(c.getStatus())
                        && !PlatformContractStatus.VOIDED.name().equals(c.getStatus()))
                .map(PlatformContract::getOrganizationId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        return DataAccessLayer.with(Organization.class)
                .selectListByLambda(new LambdaQueryWrapper<Organization>()
                        .orderByDesc(Organization::getCreateTime)).stream()
                .filter(o -> scope == null || scope.contains(o.getId()))
                .filter(o -> !OrganizationContext.DEFAULT_ORGANIZATION_ID.equals(o.getId()))
                .map(o -> {
                    PlatformOrgOptionResponse option = new PlatformOrgOptionResponse();
                    option.setId(o.getId());
                    option.setName(o.getName());
                    option.setOrgType(o.getOrgType());
                    // 带出租户当前套餐版本，前端选中租户后默认回填版本
                    TenantPlan plan = tenantPlanService.getByOrganizationId(o.getId());
                    option.setEditionCode(plan == null ? null : plan.getVersion());
                    option.setHasContract(orgsWithContract.contains(o.getId()));
                    return option;
                })
                .toList();
    }

    /**
     * 平台合同下拉选项（回款/发票选合同用）
     */
    /**
     * 取某租户当前生效的合同（最新一份已完成，无则退回最新已归档；排除草稿/待签署/作废）。
     */
    public PlatformContractResponse getActiveByOrganizationId(String organizationId) {
        PlatformContract contract = findLatest(organizationId, PlatformContractStatus.COMPLETED);
        if (contract == null) {
            contract = findLatest(organizationId, PlatformContractStatus.ARCHIVED);
        }
        if (contract == null) {
            return null;
        }
        PlatformContractResponse response = new PlatformContractResponse();
        copy(contract, response);
        response.setAttachmentList(resolveAttachments(contract.getAttachmentIds()));
        return response;
    }

    private PlatformContract findLatest(String organizationId, PlatformContractStatus status) {
        List<PlatformContract> list = contractMapper.selectListByLambda(new LambdaQueryWrapper<PlatformContract>()
                .eq(PlatformContract::getOrganizationId, organizationId)
                .eq(PlatformContract::getStatus, status.name())
                .orderByDesc(PlatformContract::getCreateTime));
        return list.isEmpty() ? null : list.getFirst();
    }

    public List<PlatformContractOptionResponse> listContractOptions() {
        Set<String> scope = scopedOrgIds();
        return contractMapper.selectListByLambda(new LambdaQueryWrapper<PlatformContract>()
                        .orderByDesc(PlatformContract::getCreateTime)).stream()
                .filter(c -> scope == null || scope.contains(c.getOrganizationId()))
                .map(c -> {
                    PlatformContractOptionResponse option = new PlatformContractOptionResponse();
                    option.setId(c.getId());
                    option.setContractNo(c.getContractNo());
                    option.setOrgName(c.getOrgName());
                    option.setEditionName(c.getEditionName());
                    option.setAmount(c.getAmount());
                    return option;
                })
                .toList();
    }

    /**
     * 保存请求 → 实体公共字段（含快照回填）
     */
    private void apply(PlatformContract contract, PlatformContractSaveRequest request) {
        contract.setContractNo(request.getContractNo());
        contract.setOrganizationId(request.getOrganizationId());
        contract.setCreditCode(request.getCreditCode());
        contract.setContactPerson(request.getContactPerson());
        contract.setContactPhone(request.getContactPhone());
        contract.setAddress(request.getAddress());
        contract.setEditionCode(request.getEditionCode());
        contract.setAmount(request.getAmount());
        contract.setValidityDays(request.getValidityDays());
        contract.setSignType(request.getSignType());
        contract.setAttachmentIds(request.getAttachmentIds());
        contract.setRemark(request.getRemark());
        // 租户主体快照
        Organization org = organizationMapper.selectByPrimaryKey(request.getOrganizationId());
        contract.setOrgName(org == null ? null : org.getName());
        // 城市经理归属快照：表单指定优先，否则继承租户已有归属（业绩口径以租户表为准）
        String signManagerId = StringUtils.isNotBlank(request.getSignManagerId())
                ? request.getSignManagerId()
                : (org == null ? null : org.getSignManagerId());
        contract.setSignManagerId(signManagerId);
        contract.setFollowManagerId(org == null ? null : org.getFollowManagerId());
        // 版本名称快照
        SysEdition edition = editionService.getEditionByCode(request.getEditionCode());
        contract.setEditionName(edition == null ? null : edition.getName());
    }

    /**
     * 首次签约自动回填租户签约经理：表单指定了签约经理且租户尚未归属时，落 sys_organization（永久，后续不覆盖）
     */
    private void assignSignManagerToOrg(PlatformContractSaveRequest request, String operatorId) {
        if (StringUtils.isBlank(request.getSignManagerId())) {
            return;
        }
        Organization org = organizationMapper.selectByPrimaryKey(request.getOrganizationId());
        if (org == null || StringUtils.isNotBlank(org.getSignManagerId())) {
            return;
        }
        org.setSignManagerId(request.getSignManagerId());
        // 首次签约自动补跟进经理：谁签的谁跟进（仅在跟进经理为空时补，不覆盖已有跟进）
        if (StringUtils.isBlank(org.getFollowManagerId())) {
            org.setFollowManagerId(request.getSignManagerId());
        }
        org.setUpdateTime(System.currentTimeMillis());
        org.setUpdateUser(operatorId);
        organizationMapper.updateById(org);
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
     * 校验租户是否在当前用户数据范围内（城市经理只能操作自己归属租户的合同）。
     */
    private void checkOrgInScope(String organizationId, Set<String> scope) {
        if (scope != null && (StringUtils.isBlank(organizationId) || !scope.contains(organizationId))) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    private PlatformContractStatus parseStatus(String status) {
        try {
            return PlatformContractStatus.valueOf(status);
        } catch (Exception e) {
            throw new GenericException(Translator.get("record.not.exist"));
        }
    }

    private void copy(PlatformContract from, PlatformContractResponse to) {
        to.setId(from.getId());
        to.setContractNo(from.getContractNo());
        to.setOrganizationId(from.getOrganizationId());
        to.setOrgName(from.getOrgName());
        to.setCreditCode(from.getCreditCode());
        to.setContactPerson(from.getContactPerson());
        to.setContactPhone(from.getContactPhone());
        to.setAddress(from.getAddress());
        to.setEditionCode(from.getEditionCode());
        to.setEditionName(from.getEditionName());
        to.setAmount(from.getAmount());
        to.setValidityDays(from.getValidityDays());
        to.setSignType(from.getSignType());
        to.setSignManagerId(from.getSignManagerId());
        to.setFollowManagerId(from.getFollowManagerId());
        to.setStatus(from.getStatus());
        to.setAttachmentIds(from.getAttachmentIds());
        to.setRemark(from.getRemark());
        to.setCreateTime(from.getCreateTime());
        to.setUpdateTime(from.getUpdateTime());
        to.setCreateUser(from.getCreateUser());
        to.setUpdateUser(from.getUpdateUser());
    }

    private List<PlatformAttachmentResponse> resolveAttachments(String idStr) {
        if (StringUtils.isBlank(idStr)) {
            return List.of();
        }
        List<String> ids = Arrays.stream(idStr.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return attachmentMapper.selectByIds(new ArrayList<>(ids)).stream()
                .map(this::toAttachmentResponse)
                .toList();
    }

    /**
     * 将逗号分隔的临时附件 ID 转存为正式附件（写 sys_attachment），并移除已删除的旧附件。
     * processTemp 按 resourceId 做「集合同步」：临时 ID 不在库里的转存新增，库里多余的删除。
     */
    private void syncAttachments(String orgId, String resourceId, String operatorId, String idStr) {
        List<String> ids = StringUtils.isBlank(idStr) ? List.of()
                : Arrays.stream(idStr.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();
        attachmentService.processTemp(new UploadTransferRequest(orgId, resourceId, operatorId, ids));
    }

    private PlatformAttachmentResponse toAttachmentResponse(Attachment a) {
        PlatformAttachmentResponse response = new PlatformAttachmentResponse();
        response.setId(a.getId());
        response.setName(a.getName());
        response.setType(a.getType());
        response.setSize(a.getSize());
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
