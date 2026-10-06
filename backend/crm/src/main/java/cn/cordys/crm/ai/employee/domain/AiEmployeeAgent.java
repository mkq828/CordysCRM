package cn.cordys.crm.ai.employee.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 员工「岗位 Agent」（功能 6，租户级）：从系统角色目录挑一个角色落到具体岗位，并附加自定义提示词与
 * 知识库检索开关。对话时以关联角色的系统提示词为底，拼接岗位自定义提示词后编排。
 */
@Data
@Table(name = "ai_employee_agent")
public class AiEmployeeAgent extends BaseModel {

    @Schema(description = "租户组织ID")
    private String organizationId;

    @Schema(description = "岗位 Agent 名称")
    private String name;

    @Schema(description = "关联员工角色(ai_employee.id)")
    private String employeeId;

    @Schema(description = "追加的自定义提示词(可选，拼在角色提示词后)")
    private String customPrompt;

    @Schema(description = "岗位描述(可选，为空时回退角色描述)")
    private String description;

    @Schema(description = "是否挂知识库检索(1=是)")
    private Integer knowledgeEnabled;

    @Schema(description = "排序")
    private Long sort;
}
