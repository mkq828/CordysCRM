package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.dto.request.CustomerContractPageRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 只读工具：查某客户的合同列表（带行级数据权限）。 */
@Service
public class SearchContractsTool implements AiAgentTool {

    @Resource
    private ContractService contractService;

    @Resource
    private DataScopeService dataScopeService;

    @Override
    public String name() {
        return "search_contracts";
    }

    @Override
    public String description() {
        return "查某客户的合同列表。参数 customerId=客户 id。返回合同阶段、负责人、已回款金额（最多 10 条）。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        Object cid = args == null ? null : args.get("customerId");
        String customerId = cid == null ? null : String.valueOf(cid);
        if (customerId == null || customerId.isBlank()) {
            return JSON.toJSONString(Map.of("error", "缺少 customerId 参数"));
        }

        CustomerContractPageRequest request = new CustomerContractPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setCustomerId(customerId);

        DeptDataPermissionDTO perm = dataScopeService.getDeptDataPermission(userId, orgId, null,
                PermissionConstants.CONTRACT_READ);
        List<ContractListResponse> list = contractService.list(request, userId, orgId, perm, false).getList();

        List<Map<String, Object>> items = new ArrayList<>();
        for (ContractListResponse c : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("customerName", c.getCustomerName());
            item.put("ownerName", c.getOwnerName());
            item.put("stageName", c.getStageName());
            item.put("alreadyPayAmount", c.getAlreadyPayAmount());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return JSON.toJSONString(result);
    }
}
