package cn.cordys.crm.customer.tag.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.tag.domain.CustomerTag;
import cn.cordys.crm.customer.tag.dto.request.CustomerTagBatchRequest;
import cn.cordys.crm.customer.tag.service.CustomerTagService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 客户标签接口：标签字典（列表筛选用）、客户已打标签（详情抽屉展示）、人工打标。
 * AI 自动打标签不走这里，由 {@code AiAgentActionExecutor} 二次复核权限后直接调服务层。
 */
@RestController
@RequestMapping("/customer/tag")
@Tag(name = "客户标签")
public class CustomerTagController {

    @Resource
    private CustomerTagService customerTagService;

    @Resource
    private ResourcePermissionService resourcePermissionService;

    @GetMapping
    @Operation(summary = "租户标签字典列表")
    public List<CustomerTag> list() {
        return customerTagService.list(OrganizationContext.getOrganizationId());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "客户已打标签列表")
    public List<CustomerTag> customerTags(@PathVariable String customerId) {
        return customerTagService.listCustomerTags(OrganizationContext.getOrganizationId(), customerId);
    }

    @PostMapping("/customer/{customerId}")
    @Operation(summary = "给客户打标签（人工）")
    public void tagCustomer(@PathVariable String customerId, @RequestParam String tagName) {
        String orgId = OrganizationContext.getOrganizationId();
        String userId = SessionUtils.getUserId();
        resourcePermissionService.checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                customerId, FormKeyConstants.CUSTOMER, userId, orgId);
        if (StringUtils.isBlank(tagName)) {
            return;
        }
        customerTagService.tagCustomer(orgId, customerId, tagName.trim(), "manual");
    }

    @PostMapping("/customer/batch")
    @Operation(summary = "批量查询客户的已打标签（列表页标签列）")
    public Map<String, List<CustomerTag>> customerTagsBatch(@RequestBody CustomerTagBatchRequest request) {
        return customerTagService.listCustomerTagsByCustomerIds(
                OrganizationContext.getOrganizationId(), request.getCustomerIds());
    }
}
