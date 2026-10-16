package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 经营大屏「主动预警」响应。
 */
@Data
@Schema(description = "主动预警响应")
public class AlertResponse {

    @Schema(description = "异常项列表")
    private List<AlertItem> items = new ArrayList<>();
}
