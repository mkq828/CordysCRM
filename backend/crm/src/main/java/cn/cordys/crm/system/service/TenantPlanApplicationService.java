package cn.cordys.crm.system.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.TenantPlanApplicationStatus;
import cn.cordys.crm.system.domain.Organization;
import cn.cordys.crm.system.domain.SysEdition;
import cn.cordys.crm.system.domain.TenantPlan;
import cn.cordys.crm.system.domain.TenantPlanApplication;
import cn.cordys.crm.system.dto.request.TenantPlanApplicationApproveRequest;
import cn.cordys.crm.system.dto.request.TenantPlanApplicationPageRequest;
import cn.cordys.crm.system.dto.request.TenantPlanApplyRequest;
import cn.cordys.crm.system.dto.response.TenantPlanApplicationResponse;
import cn.cordys.crm.system.dto.response.TenantPlanQuoteResponse;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.platform.dto.response.PlatformBankAccountResponse;
import cn.cordys.crm.platform.service.PlatformBankAccountService;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 租户续费/升级申请：租户自助提交，admin 核销后调用开通/升级
 */
@Service
@Slf4j
public class TenantPlanApplicationService {

    @Resource
    private BaseMapper<TenantPlanApplication> applicationMapper;

    @Resource
    private BaseMapper<Organization> organizationMapper;

    @Resource
    private TenantPlanService tenantPlanService;

    @Resource
    private EditionService editionService;

    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    @Resource
    private BaseMapper<Notification> notificationMapper;

    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;

    @Resource
    private PlatformBankAccountService platformBankAccountService;

    /**
     * 租户自助提交续费/升级申请
     */
    public void apply(TenantPlanApplyRequest request, String organizationId, String operatorId) {
        if (StringUtils.isBlank(organizationId)) {
            throw new GenericException(Translator.get("organization.not.exist"));
        }
        TenantPlan plan = tenantPlanService.getByOrganizationId(organizationId);
        String currentVersion = plan == null ? null : plan.getVersion();
        TenantPlanQuoteResponse quote = tenantPlanService.quote(organizationId, request.getTargetEdition());

        long now = System.currentTimeMillis();
        TenantPlanApplication application = new TenantPlanApplication();
        application.setId(IDGenerator.nextStr());
        application.setOrganizationId(organizationId);
        application.setOrgName(resolveOrgName(organizationId));
        application.setCurrentVersion(currentVersion);
        application.setTargetVersion(request.getTargetEdition());
        application.setAmount(quote.getAmount());
        application.setPriceDetail(quote.getPriceDetail());
        application.setValidityDays(quote.getValidityDays());
        application.setPaymentType(request.getPaymentType());
        application.setVoucherIds(StringUtils.trimToNull(request.getVoucherIds()));
        application.setStatus(TenantPlanApplicationStatus.PENDING.getValue());
        application.setRemark(StringUtils.trimToNull(request.getRemark()));
        application.setCreateTime(now);
        application.setUpdateTime(now);
        application.setCreateUser(operatorId);
        application.setUpdateUser(operatorId);
        applicationMapper.insert(application);

        // 站内信通知平台管理员及时核销开通（避免付费客户等候）
        Map<String, Object> resource = new HashMap<>();
        resource.put("name", application.getOrgName());
        resource.put("targetVersionName", resolveEditionName(application.getTargetVersion()));
        resource.put("amount", application.getAmount() == null ? "" : application.getAmount().toPlainString());
        commonNoticeSendService.sendNotice(
                NotificationConstants.Module.SYSTEM,
                NotificationConstants.Event.TENANT_PLAN_APPLY,
                resource,
                operatorId,
                OrganizationContext.DEFAULT_ORGANIZATION_ID,
                List.of(InternalUser.ADMIN.getValue()),
                false);
    }

    /**
     * 本租户申请记录（个人中心回显）
     */
    public List<TenantPlanApplicationResponse> listByOrganization(String organizationId) {
        if (StringUtils.isBlank(organizationId)) {
            return List.of();
        }
        List<PlatformBankAccountResponse> accounts = platformBankAccountService.list();
        return applicationMapper.selectListByLambda(new LambdaQueryWrapper<TenantPlanApplication>()
                        .eq(TenantPlanApplication::getOrganizationId, organizationId)
                        .orderByDesc(TenantPlanApplication::getCreateTime))
                .stream()
                .map(application -> toResponse(application, accounts))
                .toList();
    }

    /**
     * 管理端分页查询申请（PENDING 优先靠前端按状态筛，默认按申请时间倒序）
     */
    public List<TenantPlanApplicationResponse> pageList(TenantPlanApplicationPageRequest request) {
        LambdaQueryWrapper<TenantPlanApplication> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(request.getStatus())) {
            wrapper.eq(TenantPlanApplication::getStatus, request.getStatus());
        }
        if (StringUtils.isNotBlank(request.getKeyword())) {
            wrapper.like(TenantPlanApplication::getOrgName, request.getKeyword());
        }
        wrapper.orderByDesc(TenantPlanApplication::getCreateTime);
        return applicationMapper.selectListByLambda(wrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 核销：确认收款后开通/升级套餐（续费走 activateByOrganization，升级走 upgradeByOrganization）
     */
    public void approve(TenantPlanApplicationApproveRequest request, String operatorId) {
        TenantPlanApplication application = applicationMapper.selectByPrimaryKey(request.getId());
        if (application == null) {
            throw new GenericException(Translator.get("tenant.plan.application.not_found"));
        }
        if (!TenantPlanApplicationStatus.PENDING.getValue().equals(application.getStatus())) {
            throw new GenericException(Translator.get("tenant.plan.application.already_processed"));
        }
        if (isUpgrade(application)) {
            tenantPlanService.upgradeByOrganization(application.getOrganizationId(), application.getTargetVersion(), operatorId);
        } else {
            tenantPlanService.activateByOrganization(application.getOrganizationId(), application.getTargetVersion(),
                    application.getValidityDays(), operatorId);
        }
        application.setStatus(TenantPlanApplicationStatus.APPROVED.getValue());
        if (StringUtils.isNotBlank(request.getContractId())) {
            application.setContractId(request.getContractId());
        }
        if (StringUtils.isNotBlank(request.getRemark())) {
            application.setVerifyRemark(StringUtils.trim(request.getRemark()));
        }
        application.setUpdateTime(System.currentTimeMillis());
        application.setUpdateUser(operatorId);
        applicationMapper.updateById(application);

        // 站内信通知租户管理员核销结果
        notifyTenantResult(application, NotificationConstants.Event.TENANT_PLAN_APPROVED, operatorId);
    }

    /**
     * 驳回申请（必填驳回原因，站内信通知租户管理员）
     */
    public void cancel(TenantPlanApplicationApproveRequest request, String operatorId) {
        TenantPlanApplication application = applicationMapper.selectByPrimaryKey(request.getId());
        if (application == null) {
            throw new GenericException(Translator.get("tenant.plan.application.not_found"));
        }
        if (!TenantPlanApplicationStatus.PENDING.getValue().equals(application.getStatus())) {
            throw new GenericException(Translator.get("tenant.plan.application.already_processed"));
        }
        application.setStatus(TenantPlanApplicationStatus.CANCELLED.getValue());
        application.setVerifyRemark(StringUtils.trimToNull(request.getRemark()));
        application.setUpdateTime(System.currentTimeMillis());
        application.setUpdateUser(operatorId);
        applicationMapper.updateById(application);

        // 站内信通知租户管理员驳回原因
        notifyTenantResult(application, NotificationConstants.Event.TENANT_PLAN_REJECTED, operatorId);
    }

    /**
     * 是否升级（目标版本 tier 高于当前版本）
     */
    private boolean isUpgrade(TenantPlanApplication application) {
        SysEdition current = editionService.getEditionByCode(application.getCurrentVersion());
        SysEdition target = editionService.getEditionByCode(application.getTargetVersion());
        int currentSort = current == null || current.getSort() == null ? 0 : current.getSort();
        int targetSort = target == null || target.getSort() == null ? 0 : target.getSort();
        return targetSort > currentSort;
    }

    private String resolveOrgName(String organizationId) {
        Organization organization = organizationMapper.selectByPrimaryKey(organizationId);
        return organization == null ? null : organization.getName();
    }

    private String resolveEditionName(String editionCode) {
        if (StringUtils.isBlank(editionCode)) {
            return null;
        }
        SysEdition edition = editionService.getEditionByCode(editionCode);
        return edition == null ? editionCode : edition.getName();
    }

    private TenantPlanApplicationResponse toResponse(TenantPlanApplication application) {
        return toResponse(application, null);
    }

    private TenantPlanApplicationResponse toResponse(TenantPlanApplication application, List<PlatformBankAccountResponse> accounts) {
        TenantPlanApplicationResponse response = new TenantPlanApplicationResponse();
        response.setId(application.getId());
        response.setOrganizationId(application.getOrganizationId());
        response.setOrgName(application.getOrgName());
        response.setCurrentVersion(application.getCurrentVersion());
        response.setCurrentVersionName(resolveEditionName(application.getCurrentVersion()));
        response.setTargetVersion(application.getTargetVersion());
        response.setTargetVersionName(resolveEditionName(application.getTargetVersion()));
        response.setAmount(application.getAmount());
        response.setPriceDetail(application.getPriceDetail());
        response.setValidityDays(application.getValidityDays());
        response.setPaymentType(application.getPaymentType());
        response.setVoucherIds(application.getVoucherIds());
        response.setStatus(application.getStatus());
        response.setContractId(application.getContractId());
        response.setRemark(application.getRemark());
        response.setVerifyRemark(application.getVerifyRemark());
        response.setCreateTime(application.getCreateTime());
        if (accounts != null) {
            accounts.stream()
                    .filter(a -> Objects.equals(a.getAccountType(), application.getPaymentType()))
                    .findFirst()
                    .ifPresent(a -> {
                        response.setAccountName(a.getAccountName());
                        response.setAccountNo(a.getAccountNo());
                        response.setBankName(a.getBankName());
                        response.setQrcode(a.getQrcode());
                    });
        }
        return response;
    }

    /**
     * 通知租户管理员核销/驳回结果（站内信；短信/企微待上线前统一接入）
     * 通知框架按 organization_id 匹配模板、跨组织无法送达，故参照注册审核直接落库到租户组织下
     */
    private void notifyTenantResult(TenantPlanApplication application, String event, String operatorId) {
        if (StringUtils.isBlank(application.getCreateUser())) {
            return;
        }
        MessageTask task = extMessageTaskMapper.getMessageByEvent(event, OrganizationContext.DEFAULT_ORGANIZATION_ID);
        if (task == null) {
            log.warn("平台组织未配置续费/升级结果通知模板，跳过通知：{}", event);
            return;
        }
        if (!Boolean.TRUE.equals(task.getSysEnable())) {
            return;
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", application.getOrgName());
        paramMap.put("remark", StringUtils.defaultString(application.getVerifyRemark()));
        String content = MessageTemplateUtils.getContent(resolveTemplate(task), paramMap);

        long now = System.currentTimeMillis();
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setType(NotificationConstants.Type.SYSTEM_NOTICE.name());
        notification.setReceiver(application.getCreateUser());
        notification.setSubject(MessageTemplateUtils.getEventMap().get(event));
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOperator(operatorId);
        notification.setOperation(event);
        notification.setOrganizationId(application.getOrganizationId());
        notification.setResourceType(NotificationConstants.Module.SYSTEM);
        notification.setResourceName(application.getOrgName());
        notification.setContent(content.getBytes(StandardCharsets.UTF_8));
        notification.setCreateUser(operatorId);
        notification.setUpdateUser(operatorId);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
    }

    private String resolveTemplate(MessageTask task) {
        byte[] template = task.getTemplate();
        if (template != null && template.length > 0) {
            return new String(template, StandardCharsets.UTF_8);
        }
        return MessageTemplateUtils.getTemplate(task.getEvent());
    }
}
