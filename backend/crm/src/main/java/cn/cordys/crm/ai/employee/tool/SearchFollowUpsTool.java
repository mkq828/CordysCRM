package cn.cordys.crm.ai.employee.tool;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.follow.dto.request.FollowUpRecordPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpRecordListResponse;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 只读工具：查某客户的跟进记录（列表入口已带 userId/orgId 数据权限过滤）。 */
@Service
public class SearchFollowUpsTool implements AiAgentTool {

    @Resource
    private FollowUpRecordService followUpRecordService;

    @Override
    public String name() {
        return "search_follow_ups";
    }

    @Override
    public String description() {
        return "查某客户的跟进记录列表。参数 customerId=客户 id。返回跟进类型、跟进方式、跟进时间、跟进人、内容摘要（最多 10 条）。";
    }

    @Override
    public String execute(String orgId, String userId, Map<String, Object> args) {
        Object cid = args == null ? null : args.get("customerId");
        String customerId = cid == null ? null : String.valueOf(cid);
        if (customerId == null || customerId.isBlank()) {
            return JSON.toJSONString(Map.of("error", "缺少 customerId 参数"));
        }

        FollowUpRecordPageRequest request = new FollowUpRecordPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setSourceId(customerId);

        List<FollowUpRecordListResponse> list = followUpRecordService
                .list(request, userId, orgId, "customer", null).getList();

        List<Map<String, Object>> items = new ArrayList<>();
        for (FollowUpRecordListResponse r : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", r.getType());
            item.put("followMethod", r.getFollowMethod());
            item.put("followTime", r.getFollowTime());
            item.put("ownerName", r.getOwnerName());
            item.put("content", StringUtils.abbreviate(r.getContent(), 120));
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return JSON.toJSONString(result);
    }
}
