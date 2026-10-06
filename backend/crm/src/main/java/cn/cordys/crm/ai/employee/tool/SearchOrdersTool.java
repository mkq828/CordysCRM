package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.customer.dto.request.CustomerOrderPageRequest;
import cn.cordys.crm.order.dto.response.OrderListResponse;
import cn.cordys.crm.order.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 只读工具：查某客户的订单列表（带行级数据权限）。 */
@Service
public class SearchOrdersTool implements AiAgentTool {

    @Resource
    private OrderService orderService;

    @Resource
    private DataScopeService dataScopeService;

    @Override
    public String name() {
        return "search_orders";
    }

    @Override
    public String description() {
        return "查某客户的订单列表。参数 customerId=客户 id。返回关联合同名、负责人、订单阶段（最多 10 条）。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        Object cid = args == null ? null : args.get("customerId");
        String customerId = cid == null ? null : String.valueOf(cid);
        if (customerId == null || customerId.isBlank()) {
            return JSON.toJSONString(Map.of("error", "缺少 customerId 参数"));
        }

        CustomerOrderPageRequest request = new CustomerOrderPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setCustomerId(customerId);

        DeptDataPermissionDTO perm = dataScopeService.getDeptDataPermission(userId, orgId, null,
                PermissionConstants.ORDER_READ);
        List<OrderListResponse> list = orderService.list(request, userId, orgId, perm, false).getList();

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderListResponse o : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("customerName", o.getCustomerName());
            item.put("contractName", o.getContractName());
            item.put("ownerName", o.getOwnerName());
            item.put("stageName", o.getStageName());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return JSON.toJSONString(result);
    }
}
