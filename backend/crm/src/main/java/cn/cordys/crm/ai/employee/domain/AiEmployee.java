package cn.cordys.crm.ai.employee.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * AI 员工角色目录（功能 6，系统级）：来源于开源项目 agency-agents（MIT，保留署名）的 264 个岗位角色，
 * 按部门（division）沉淀角色名、描述、品牌色、emoji、一句话人设与角色系统提示词。
 */
@Data
@Table(name = "ai_employee")
public class AiEmployee extends BaseModel {

    @Schema(description = "部门英文标识(sales/marketing/engineering...)")
    private String division;

    @Schema(description = "员工角色名")
    private String name;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "品牌色(hex)")
    private String color;

    @Schema(description = "emoji 图标")
    private String emoji;

    @Schema(description = "一句话人设")
    private String vibe;

    @Schema(description = "角色提示词(系统提示，Markdown)")
    private String systemPrompt;

    @Schema(description = "同部门内排序")
    private Long sort;
}
