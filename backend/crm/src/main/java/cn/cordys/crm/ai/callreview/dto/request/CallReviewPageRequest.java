package cn.cordys.crm.ai.callreview.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通话复盘分页请求（可选按状态过滤）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallReviewPageRequest extends BasePageRequest {

    /** 状态过滤（可空） */
    private String status;
}
