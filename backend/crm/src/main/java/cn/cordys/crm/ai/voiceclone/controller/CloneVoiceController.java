package cn.cordys.crm.ai.voiceclone.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.voiceclone.dto.request.CloneVoicePreviewRequest;
import cn.cordys.crm.ai.voiceclone.dto.request.CloneVoiceSubmitRequest;
import cn.cordys.crm.ai.voiceclone.dto.response.CloneVoicePreviewResponse;
import cn.cordys.crm.ai.voiceclone.dto.response.CloneVoiceResponse;
import cn.cordys.crm.ai.voiceclone.service.CloneVoiceService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 声音克隆接口（功能 11，企业版 G3 门控，只挂版本开关不挂 G2 额度）。
 * 复刻走异步训练，前端每 3-5s 轮询 /tasks 感知进度。
 */
@RestController
@RequestMapping("/agent/voice-clone")
@Tag(name = "声音克隆")
public class CloneVoiceController {

    @Resource
    private CloneVoiceService cloneVoiceService;
    @Resource
    private EditionService editionService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通声音克隆")
    public boolean available() {
        return editionService.hasFeature(OrganizationContext.getOrganizationId(), AiQuotaConstant.AI_VOICE_CLONE);
    }

    @PostMapping("/tasks")
    @Operation(summary = "提交声音复刻（异步训练）")
    public CloneVoiceResponse submit(@Valid @RequestBody CloneVoiceSubmitRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        CloneVoiceResponse response = cloneVoiceService.submit(request, orgId, SessionUtils.getUserId());
        cloneVoiceService.trainAsync(response.getId());
        return response;
    }

    @GetMapping("/tasks")
    @Operation(summary = "音色列表（轮询进度）")
    public List<CloneVoiceResponse> list() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return cloneVoiceService.list(orgId);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "音色详情")
    public CloneVoiceResponse get(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return cloneVoiceService.get(orgId, id);
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除音色（同时清理样本附件）")
    public void delete(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        cloneVoiceService.delete(orgId, id);
    }

    @PostMapping("/tasks/{id}/preview")
    @Operation(summary = "克隆音色试听（返回 base64 WAV）")
    public CloneVoicePreviewResponse preview(@PathVariable String id,
                                             @RequestBody(required = false) CloneVoicePreviewRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String text = request == null ? null : request.getText();
        return cloneVoiceService.preview(orgId, id, text);
    }

    private void checkAvailable(String orgId) {
        if (!editionService.hasFeature(orgId, AiQuotaConstant.AI_VOICE_CLONE)) {
            throw new GenericException("当前版本未开通声音克隆，请升级至企业版");
        }
    }
}
