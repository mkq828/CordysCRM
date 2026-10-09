package cn.cordys.crm.ai.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.ai.domain.AiQuotaPack;
import cn.cordys.crm.ai.dto.request.AiQuotaPackOrderHandleRequest;
import cn.cordys.crm.ai.dto.request.AiQuotaPackOrderListRequest;
import cn.cordys.crm.ai.dto.request.AiQuotaPackSaveRequest;
import cn.cordys.crm.ai.dto.response.AiQuotaPackOrderResponse;
import cn.cordys.crm.ai.dto.response.AiQuotaPackResponse;
import cn.cordys.crm.ai.service.AiQuotaPackService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 额度包管理（平台端，仅 admin）
 */
@RestController
@RequestMapping("/admin/ai-quota-pack")
@Tag(name = "AI 额度包管理")
public class AdminAiQuotaPackController {

    @Resource
    private AiQuotaPackService aiQuotaPackService;

    @GetMapping("/packs")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_READ)
    @Operation(summary = "额度包商品列表(含停用)")
    public List<AiQuotaPackResponse> packs() {
        return aiQuotaPackService.listPackResponses(false);
    }

    @PostMapping("/pack/save")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "额度包商品保存")
    public void packSave(@Validated @RequestBody AiQuotaPackSaveRequest request) {
        AiQuotaPack pack = new AiQuotaPack();
        pack.setId(request.getId());
        pack.setFeatureCode(request.getFeatureCode());
        pack.setName(request.getName());
        pack.setCalls(request.getCalls());
        pack.setPriceFen(request.getPriceFen());
        pack.setStatus(request.getStatus());
        pack.setSort(request.getSort());
        pack.setRemark(request.getRemark());
        aiQuotaPackService.savePack(pack, SessionUtils.getUserId());
    }

    @PostMapping("/pack/delete")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "额度包商品删除")
    public void packDelete(@RequestParam("id") String id) {
        aiQuotaPackService.deletePack(id);
    }

    @PostMapping("/orders")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_READ)
    @Operation(summary = "额度包订单列表")
    public List<AiQuotaPackOrderResponse> orders(@RequestBody AiQuotaPackOrderListRequest request) {
        return aiQuotaPackService.listOrderResponses(request.getKeyword(), request.getStatus());
    }

    @PostMapping("/order/confirm")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "订单确认到账")
    public void orderConfirm(@Validated @RequestBody AiQuotaPackOrderHandleRequest request) {
        aiQuotaPackService.confirmOrder(request.getOrderId(), SessionUtils.getUserId());
    }

    @PostMapping("/order/cancel")
    @CsPermission(PermissionConstants.ADMIN_AI_QUOTA_WRITE)
    @Operation(summary = "订单取消")
    public void orderCancel(@Validated @RequestBody AiQuotaPackOrderHandleRequest request) {
        aiQuotaPackService.cancelOrder(request.getOrderId(), SessionUtils.getUserId(), request.getRemark());
    }
}
