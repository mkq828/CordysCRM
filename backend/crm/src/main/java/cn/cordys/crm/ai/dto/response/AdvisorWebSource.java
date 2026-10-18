package cn.cordys.crm.ai.dto.response;

import lombok.Data;

/**
 * 军师联网检索到的一条公开信息来源（标题 + 链接 + 摘要），供前端渲染可点击出处。
 */
@Data
public class AdvisorWebSource {

    /** 结果标题 */
    private String title;

    /** 原文链接 */
    private String url;

    /** 短摘要（snippet 为空时回退 summary） */
    private String snippet;
}
