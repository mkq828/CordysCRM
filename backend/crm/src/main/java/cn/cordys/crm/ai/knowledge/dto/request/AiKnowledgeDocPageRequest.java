package cn.cordys.crm.ai.knowledge.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业知识库文档分页请求（keyword 继承自 BaseCondition，匹配文档名）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiKnowledgeDocPageRequest extends BasePageRequest {
}
