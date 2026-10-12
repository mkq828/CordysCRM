package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 生成分集口播配音请求（指定分镜，空则整集含台词分镜）。
 */
@Data
public class DramaNarrationRequest {

    @Schema(description = "指定分镜ID列表（空则整集所有含台词的分镜）")
    private List<String> fragmentIds;

    @Schema(description = "音色标识（空则用模型默认音色）")
    private String voice;
}
