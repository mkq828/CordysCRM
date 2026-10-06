package cn.cordys.crm.ai.employee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 员工岗位 Agent 列表项/详情（含关联角色名、emoji 与品牌色，供前端展示）。
 */
@Data
public class AiEmployeeAgentResponse {

    @Schema(description = "岗位 Agent ID")
    private String id;

    @Schema(description = "岗位 Agent 名称")
    private String name;

    @Schema(description = "关联员工角色ID")
    private String employeeId;

    @Schema(description = "关联角色名")
    private String employeeName;

    @Schema(description = "关联角色 emoji")
    private String employeeEmoji;

    @Schema(description = "关联角色品牌色")
    private String employeeColor;

    @Schema(description = "追加的自定义提示词")
    private String customPrompt;

    @Schema(description = "岗位描述(添加时填写；为空时前端回退到 employeeDescription)")
    private String description;

    @Schema(description = "关联角色描述(角色目录里该岗位的描述，作为 description 为空时的回退)")
    private String employeeDescription;

    @Schema(description = "是否挂知识库检索(1=是)")
    private Integer knowledgeEnabled;

    @Schema(description = "排序")
    private Long sort;

    @Schema(description = "创建时间(毫秒)")
    private Long createTime;

    @Schema(description = "更新时间(毫秒)")
    private Long updateTime;
}
