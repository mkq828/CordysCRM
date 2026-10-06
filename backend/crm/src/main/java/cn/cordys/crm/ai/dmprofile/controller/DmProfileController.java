package cn.cordys.crm.ai.dmprofile.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.dmprofile.dto.request.DmAccountBindRequest;
import cn.cordys.crm.ai.dmprofile.dto.request.DmProfileImportRequest;
import cn.cordys.crm.ai.dmprofile.dto.response.DmAccountResponse;
import cn.cordys.crm.ai.dmprofile.dto.response.DmImportResponse;
import cn.cordys.crm.ai.dmprofile.dto.response.DmProfileResponse;
import cn.cordys.crm.ai.dmprofile.service.DmProfileService;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 私信获客画像接口（功能 8，专业版及以上 G3 门控）。
 * 账号绑定走 Cookie 登录态迁移；采集由 Python 微服务完成，画像走 CRM G1 模型层挂 G2 额度。
 */
@RestController
@RequestMapping("/agent/dm-profile")
@Tag(name = "私信获客画像")
public class DmProfileController {

    @Resource
    private DmProfileService dmProfileService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通私信获客画像")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_DM_PROFILE);
    }

    @GetMapping("/accounts")
    @Operation(summary = "已绑定的私信平台账号列表")
    public List<DmAccountResponse> accounts() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dmProfileService.listAccounts(orgId);
    }

    @PostMapping("/accounts")
    @Operation(summary = "绑定私信平台账号（提交登录态 Cookie）")
    public DmAccountResponse bind(@Valid @RequestBody DmAccountBindRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dmProfileService.bindAccount(orgId, SessionUtils.getUserId(), request);
    }

    @DeleteMapping("/accounts/{id}")
    @Operation(summary = "解绑私信平台账号（清空登录态）")
    public void unbind(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        dmProfileService.unbind(orgId, id);
    }

    @PostMapping("/accounts/{id}/crawl")
    @Operation(summary = "触发采集并画像（异步执行，立即返回「采集中」，前端轮询账号状态与画像列表感知进度）")
    public DmAccountResponse crawl(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        DmAccountResponse response = dmProfileService.startCrawl(orgId, id, userId);
        dmProfileService.crawlAsync(id, orgId, userId);
        return response;
    }

    @GetMapping("/profiles")
    @Operation(summary = "私信画像结果列表（可按状态筛选）")
    public List<DmProfileResponse> profiles(@RequestParam(required = false) String status) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dmProfileService.listProfiles(orgId, status);
    }

    @PostMapping("/profiles/import")
    @Operation(summary = "把选中画像导入线索池")
    public DmImportResponse importProfiles(@Valid @RequestBody DmProfileImportRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return dmProfileService.importProfiles(orgId, SessionUtils.getUserId(), request);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_DM_PROFILE)) {
            throw new GenericException("当前版本未开通私信获客画像，请升级至专业版");
        }
    }
}
