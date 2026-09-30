package cn.cordys.crm.ai.model.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 模型分页请求（keyword 继承自 BaseCondition）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentModelPageRequest extends BasePageRequest {
}
