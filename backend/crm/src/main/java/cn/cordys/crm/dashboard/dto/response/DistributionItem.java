package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分布项（名称 + 数量）。
 */
@Data
public class DistributionItem {

    @Schema(description = "分组编码（如合同阶段原始 stage id，前端据此拼 /stage/{code}）")
    private String code;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "数量")
    private long value;
}
