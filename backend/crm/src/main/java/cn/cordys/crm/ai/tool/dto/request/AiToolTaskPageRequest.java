package cn.cordys.crm.ai.tool.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 工具任务分页请求（生成记录列表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiToolTaskPageRequest extends BasePageRequest {
}
