package cn.cordys.crm.ai.advisor.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 军师查数问答响应：自然语言回答 + 可折叠的数据来源。
 */
@Data
public class AdvisorQueryResponse {

    @Schema(description = "自然语言回答")
    private String answer;

    @Schema(description = "是否识别为查数问题（false 表示问题不属于可查数据范围）")
    private boolean queried;

    @Schema(description = "数据来源列名")
    private List<String> columns;

    @Schema(description = "数据来源行（已做姓名/时间可读化）")
    private List<List<Object>> rows;

    @Schema(description = "会话 id（供前端续写同一会话）")
    private String conversationId;
}
