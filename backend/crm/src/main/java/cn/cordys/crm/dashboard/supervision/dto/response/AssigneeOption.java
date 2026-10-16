package cn.cordys.crm.dashboard.supervision.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 督办收件人候选（下拉选项）。
 */
@Data
@Schema(description = "督办收件人候选")
public class AssigneeOption {

    @Schema(description = "被督办人 userId")
    private String owner;

    @Schema(description = "被督办人姓名")
    private String ownerName;
}
