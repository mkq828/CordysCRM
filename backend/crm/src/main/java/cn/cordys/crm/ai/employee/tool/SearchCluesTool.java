package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.service.ClueService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 只读工具：按关键字搜索线索（带行级数据权限）。 */
@Service
public class SearchCluesTool implements AiAgentTool {

    @Resource
    private ClueService clueService;

    @Resource
    private DataScopeService dataScopeService;

    @Override
    public String name() {
        return "search_clues";
    }

    @Override
    public String description() {
        return "按关键字搜索线索。参数 keyword=搜索关键字。返回匹配线索的 id 和名称列表（最多 10 条）。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        Object kw = args == null ? null : args.get("keyword");
        String keyword = kw == null ? "" : String.valueOf(kw);

        CluePageRequest request = new CluePageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setKeyword(keyword);

        DeptDataPermissionDTO perm = dataScopeService.getDeptDataPermission(userId, orgId, null,
                PermissionConstants.CLUE_MANAGEMENT_READ);
        List<ClueListResponse> list = clueService.list(request, userId, orgId, perm, false).getList();

        List<Map<String, Object>> items = new ArrayList<>();
        for (ClueListResponse c : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return JSON.toJSONString(result);
    }
}
