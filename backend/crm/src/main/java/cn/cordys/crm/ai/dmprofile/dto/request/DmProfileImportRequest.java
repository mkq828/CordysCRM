package cn.cordys.crm.ai.dmprofile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 把选中的画像结果导入线索池。
 */
@Data
public class DmProfileImportRequest {

    @Schema(description = "画像结果主键列表")
    @NotEmpty(message = "请选择要导入的画像")
    private List<String> profileIds;
}
