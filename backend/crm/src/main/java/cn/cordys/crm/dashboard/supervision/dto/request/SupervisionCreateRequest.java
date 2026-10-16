package cn.cordys.crm.dashboard.supervision.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 下达督办请求。
 */
@Data
@Schema(description = "下达督办请求")
public class SupervisionCreateRequest {

    @Schema(description = "被督办人 userId")
    private String assignee;

    @Schema(description = "督办内容")
    private String content;

    @Schema(description = "截止时间(毫秒)")
    private Long deadline;
}
