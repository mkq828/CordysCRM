package cn.cordys.crm.ai.drama.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 保存分集分镜列表请求。
 */
@Data
public class DramaFragmentsSaveRequest {

    @NotEmpty(message = "分镜不能为空")
    @Schema(description = "分镜列表")
    private List<FragmentItem> fragments;

    /**
     * 单个分镜的保存项。
     */
    @Data
    public static class FragmentItem {

        @Schema(description = "分镜ID（编辑已有分镜时带，新增为空）")
        private String id;

        @Schema(description = "镜序")
        private Integer sortOrder;

        @Schema(description = "画面描述")
        private String content;

        @Schema(description = "口播台词")
        private String narration;

        @Schema(description = "本镜口播音色标识(空则用分集默认音色)")
        private String voice;

        @Schema(description = "时长(秒)")
        private Integer durationSec;

        @Schema(description = "引用资产ID数组 JSON")
        private String assetIdsJson;
    }
}
