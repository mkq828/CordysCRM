package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 功能（可配置功能点，含全局开关）
 */
@Data
@Table(name = "sys_feature")
public class SysFeature extends BaseModel {

    @Schema(description = "功能编码")
    private String featureCode;

    @Schema(description = "功能名称")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "全局开关(1启用 0停用)")
    private Boolean enable;
}
