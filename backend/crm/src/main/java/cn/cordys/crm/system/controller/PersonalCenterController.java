package cn.cordys.crm.system.controller;

import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.platform.dto.response.PlatformBankAccountResponse;
import cn.cordys.crm.system.dto.request.PersonalInfoRequest;
import cn.cordys.crm.system.dto.request.PersonalPasswordRequest;
import cn.cordys.crm.system.dto.request.SendEmailDTO;
import cn.cordys.crm.system.dto.request.TenantPlanApplyRequest;
import cn.cordys.crm.system.dto.response.EditionResponse;
import cn.cordys.crm.system.dto.response.TenantPlanApplicationResponse;
import cn.cordys.crm.system.dto.response.TenantPlanQuoteResponse;
import cn.cordys.crm.system.dto.response.TenantSubscriptionResponse;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.service.PersonalCenterService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/personal/center")
@Tag(name = "个人中心")
public class PersonalCenterController {
    @Resource
    private PersonalCenterService personalCenterService;


    @GetMapping("/info")
    @Operation(summary = "当前用户详情")
    public UserResponse getUserDetail() {
        return personalCenterService.getUserDetail(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }


    @GetMapping("/subscription")
    @Operation(summary = "当前租户套餐与合同")
    public TenantSubscriptionResponse getSubscription() {
        return personalCenterService.getSubscription(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/plan/editions")
    @Operation(summary = "可选版本列表（租户自助续费/升级）")
    public List<EditionResponse> listPlanEditions() {
        return personalCenterService.listPlanEditions();
    }

    @GetMapping("/plan/quote")
    @Operation(summary = "续费/升级报价")
    public TenantPlanQuoteResponse quote(@RequestParam("targetEdition") String targetEdition) {
        return personalCenterService.quote(OrganizationContext.getOrganizationId(), targetEdition);
    }

    @PostMapping("/plan/apply")
    @Operation(summary = "提交续费/升级申请")
    public void apply(@Validated @RequestBody TenantPlanApplyRequest request) {
        personalCenterService.applyPlan(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/plan/application/list")
    @Operation(summary = "本租户续费/升级申请记录")
    public List<TenantPlanApplicationResponse> listPlanApplications() {
        return personalCenterService.listPlanApplications(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/plan/payment-accounts")
    @Operation(summary = "收款账户（租户自助续费/升级展示收款码与对公信息）")
    public List<PlatformBankAccountResponse> listPlanPaymentAccounts() {
        return personalCenterService.listPlanPaymentAccounts();
    }


    @PostMapping("/update")
    @Operation(summary = "更新户详情")
    public UserResponse updateInfo(@Validated @RequestBody PersonalInfoRequest personalInfoRequest) {
        return personalCenterService.updateInfo(personalInfoRequest, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    /**
     * 发送验证码
     */
    @PostMapping("/mail/code/send")
    public void sendCode(@RequestBody @NotNull SendEmailDTO email) {
        personalCenterService.sendCode(email, OrganizationContext.getOrganizationId());
    }

    /**
     * 更新密码
     */
    @PostMapping("/info/reset")
    @Operation(summary = "用户密码重置")
    public void resetUserPassword(@Validated @RequestBody PersonalPasswordRequest personalPasswordRequest) {
        personalCenterService.resetUserPassword(personalPasswordRequest, SessionUtils.getUserId());
    }


    @PostMapping("/follow/plan/list")
    @Operation(summary = "用户跟进计划列表")
    public PagerWithOption<List<FollowUpPlanListResponse>> list(@Validated @RequestBody FollowUpPlanPageRequest request) {
        request.setMyPlan(true);
        return personalCenterService.getPlanList(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
