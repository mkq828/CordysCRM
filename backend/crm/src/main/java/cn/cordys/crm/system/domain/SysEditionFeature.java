package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 版本-功能关联（多对多）
 */
@Data
@Table(name = "sys_edition_feature")
public class SysEditionFeature extends BaseModel {

    @Schema(description = "版本ID")
    private String editionId;

    @Schema(description = "功能ID")
    private String featureId;
}
