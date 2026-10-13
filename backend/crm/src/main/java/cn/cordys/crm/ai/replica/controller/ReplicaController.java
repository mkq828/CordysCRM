package cn.cordys.crm.ai.replica.controller;

import cn.cordys.common.exception.GenericException;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.ai.replica.dto.request.ReplicaSubmitRequest;
import cn.cordys.crm.ai.replica.dto.response.ReplicaTaskResponse;
import cn.cordys.crm.ai.replica.service.ReplicaGenerateService;
import cn.cordys.crm.platform.util.PlatformSessionUtils;
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
 * 爆款复刻接口（功能 7 扩展 · 换皮变体，专业版+ G3 门控，变体方案走 LLM 挂 G2 额度）。
 * 解析/生成/合成走异步任务，前端每 3s 轮询 /tasks 感知进度。
 */
@RestController
@RequestMapping("/agent/replica")
@Tag(name = "爆款复刻")
public class ReplicaController {

    @Resource
    private ReplicaGenerateService replicaGenerateService;

    @GetMapping("/available")
    @Operation(summary = "查询当前租户是否开通爆款复刻")
    public boolean available() {
        // 爆款复刻已封存：仅平台超级管理员（userId == admin）可用
        return PlatformSessionUtils.isAdmin();
    }

    @PostMapping("/tasks")
    @Operation(summary = "提交爆款复刻任务（异步解析+生成变体+合成）")
    public ReplicaTaskResponse create(@Valid @RequestBody ReplicaSubmitRequest request) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        String userId = SessionUtils.getUserId();
        ReplicaTaskResponse response = replicaGenerateService.submit(request, orgId, userId);
        replicaGenerateService.processAsync(response.getId(), orgId, userId);
        return response;
    }

    @GetMapping("/tasks")
    @Operation(summary = "任务列表（轮询进度）")
    public List<ReplicaTaskResponse> list() {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return replicaGenerateService.list(orgId);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "任务详情（含变体列表）")
    public ReplicaTaskResponse get(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        return replicaGenerateService.get(orgId, id);
    }

    @PostMapping("/tasks/{id}/regenerate")
    @Operation(summary = "重新生成变体")
    public void regenerate(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        replicaGenerateService.regenerateAsync(id, orgId, SessionUtils.getUserId());
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除任务（同时清理附件）")
    public void delete(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        replicaGenerateService.delete(orgId, id);
    }

    @DeleteMapping("/variants/{id}")
    @Operation(summary = "删除单个变体")
    public void deleteVariant(@PathVariable String id) {
        String orgId = OrganizationContext.getOrganizationId();
        checkAvailable(orgId);
        replicaGenerateService.deleteVariant(orgId, id);
    }

    private void checkAvailable(String orgId) {
        // 爆款复刻已封存，仅平台超级管理员可用；orgId 保留参数以对齐各调用点签名
        if (!PlatformSessionUtils.isAdmin()) {
            throw new GenericException("爆款复刻功能已封存，暂未对外开放");
        }
    }
}
