package cn.cordys.crm.ai.callreview.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 一键转跟进成功后回写跟进记录ID，用于详情禁用重复转跟进。
 */
@Data
public class CallReviewFollowRequest {

    @Schema(description = "跟进记录ID")
    private String followRecordId;
}
