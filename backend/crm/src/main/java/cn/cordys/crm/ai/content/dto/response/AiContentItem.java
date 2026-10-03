package cn.cordys.crm.ai.content.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单条获客内容：选题 + 爆款文案 + 配图文案。
 */
@Data
public class AiContentItem {

    @Schema(description = "选题（一句话角度）")
    private String topic;

    @Schema(description = "爆款文案（正文）")
    private String copy;

    @Schema(description = "配图文案（封面/配图上的短文案）")
    private String imageCopy;
}
