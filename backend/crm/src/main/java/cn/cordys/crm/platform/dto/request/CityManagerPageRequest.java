package cn.cordys.crm.platform.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 城市经理分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CityManagerPageRequest extends BasePageRequest {

    @Schema(description = "关键字(姓名/手机号)")
    private String keyword;

    @Schema(description = "状态(ENABLED/DISABLED)")
    private String status;
}
