package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.service.CustomerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读工具：按关键字搜索客户（带行级数据权限）。
 * <p>
 * 除精确的「名称包含关键字」外，还支持「简称/别称」模糊：当输入像「深圳智造」这种非连续子串时，按字符顺序
 * 子序列匹配到「深圳市智造未来科技有限公司」。命中多个候选时不自行裁决，由上游把候选列给用户选择。
 * </p>
 */
@Service
public class SearchCustomersTool implements AiAgentTool {

    /** 候选池上限，用于简称模糊时的预筛 */
    private static final int CANDIDATE_LIMIT = 50;

    @Resource
    private CustomerService customerService;

    @Resource
    private DataScopeService dataScopeService;

    @Override
    public String name() {
        return "search_customers";
    }

    @Override
    public String description() {
        return "按关键字搜索客户（公司名称）。参数 keyword=搜索关键字。返回匹配客户的 id 和名称列表（最多 10 条）；"
                + "命中多个候选时，请向用户列出候选并请其选择，不要自行猜测。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        Object kw = args == null ? null : args.get("keyword");
        String keyword = kw == null ? "" : String.valueOf(kw).trim();
        if (keyword.isEmpty()) {
            return JSON.toJSONString(Map.of("error", "缺少 keyword 参数"));
        }

        List<Map<String, Object>> items = search(keyword, orgId, userId, 10);
        if (items.isEmpty()) {
            items = searchSubsequence(keyword, orgId, userId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return JSON.toJSONString(result);
    }

    /** 精确模糊：名称包含关键字（SQL like %keyword%），带行级权限 */
    private List<Map<String, Object>> search(String keyword, String orgId, String userId, int pageSize) {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);

        DeptDataPermissionDTO perm = dataScopeService.getDeptDataPermission(userId, orgId, null,
                PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        List<CustomerListResponse> list = customerService.list(request, userId, orgId, perm).getList();
        return toItems(list);
    }

    /** 简称模糊：先按前两个字符预筛候选池，再按「关键字字符顺序子序列」过滤，兼容「深圳智造」→「深圳市智造未来科技有限公司」 */
    private List<Map<String, Object>> searchSubsequence(String keyword, String orgId, String userId) {
        String q = keyword.replaceAll("\\s+", "");
        if (q.isEmpty()) {
            return List.of();
        }
        String seed = q.substring(0, Math.min(2, q.length()));
        List<CustomerListResponse> candidates = searchRaw(seed, orgId, userId, CANDIDATE_LIMIT);

        List<Map<String, Object>> items = new ArrayList<>();
        for (CustomerListResponse c : candidates) {
            if (isSubsequence(q, c.getName())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", c.getId());
                item.put("name", c.getName());
                items.add(item);
                if (items.size() >= 10) {
                    break;
                }
            }
        }
        return items;
    }

    private List<CustomerListResponse> searchRaw(String keyword, String orgId, String userId, int pageSize) {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);

        DeptDataPermissionDTO perm = dataScopeService.getDeptDataPermission(userId, orgId, null,
                PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        return customerService.list(request, userId, orgId, perm).getList();
    }

    private List<Map<String, Object>> toItems(List<CustomerListResponse> list) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (CustomerListResponse c : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            items.add(item);
        }
        return items;
    }

    /** 判断 q 的每个字符是否按顺序出现在 name 中（非连续子串匹配） */
    private boolean isSubsequence(String q, String name) {
        if (q == null || name == null) {
            return false;
        }
        int i = 0;
        for (int j = 0; j < name.length() && i < q.length(); j++) {
            if (name.charAt(j) == q.charAt(i)) {
                i++;
            }
        }
        return i == q.length();
    }
}
