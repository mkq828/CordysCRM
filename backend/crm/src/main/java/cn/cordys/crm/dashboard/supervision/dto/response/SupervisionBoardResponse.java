package cn.cordys.crm.dashboard.supervision.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 督办看板：任务列表 + 收件人候选。
 */
@Data
@Schema(description = "督办看板")
public class SupervisionBoardResponse {

    @Schema(description = "督办任务列表")
    private List<SupervisionItem> items = new ArrayList<>();

    @Schema(description = "收件人候选")
    private List<AssigneeOption> assignees = new ArrayList<>();
}
