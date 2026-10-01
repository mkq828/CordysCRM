package cn.cordys.crm.platform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台系统信息（关于弹窗）：运营方名称
 */
@Data
public class SystemInfoResponse {

    @Schema(description = "运营方名称(未配置为 null，前端回退默认)")
    private String operator;
}
