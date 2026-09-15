package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 注册申请单分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterApplicationPageRequest extends BasePageRequest {

    @Schema(description = "注册类型(PERSONAL/ENTERPRISE)")
    private String type;

    @Schema(description = "审核状态(PENDING/APPROVED/REJECTED)")
    private String verifyStatus;

    @Schema(description = "关键字(名称/手机号)")
    private String keyword;
}
