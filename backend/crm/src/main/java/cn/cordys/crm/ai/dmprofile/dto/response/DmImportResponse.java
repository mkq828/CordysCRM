package cn.cordys.crm.ai.dmprofile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 画像导入线索池结果：逐条导入，单条失败不阻断其余。
 */
@Data
public class DmImportResponse {

    @Schema(description = "成功条数")
    private int successCount;

    @Schema(description = "因无联系方式(手机号)被过滤、未导入的条数")
    private int filteredCount;

    @Schema(description = "失败的联系人昵称")
    private List<String> failedNames;
}
