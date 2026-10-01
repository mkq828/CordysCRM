package cn.cordys.crm.platform.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 平台系统信息保存请求
 */
@Data
public class SystemInfoRequest {

    @Schema(description = "运营方名称(空则清除，回退默认)")
    private String operator;
}
