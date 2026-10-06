package cn.cordys.crm.ai.dmprofile.dto;

import lombok.Data;

import java.util.List;

/**
 * 采集任务结果（轮询 GET /api/crawl/result/{taskId} 的返回）。
 *
 * <p>{@code status} 为 queued/running 表示任务未完成；done/failed 携带后续字段。
 * {@code loggedIn=false} 表示 Cookie 已失效，需要重新绑定。</p>
 */
@Data
public class DmCollectResult {

    /** 任务状态：queued / running / done / failed / unknown */
    private String status;

    /** 是否采集成功 */
    private boolean ok;

    /** Cookie 注入后是否仍为登录态（false 表示登录态过期） */
    private boolean loggedIn;

    /** 采集到的原始私信会话 */
    private List<DmRawConversation> convs;

    /** 错误或提示信息 */
    private String message;

    /** 是否任务已完成（done 或 failed 都算结束，可停止轮询） */
    public boolean isFinished() {
        return "done".equals(status) || "failed".equals(status);
    }
}
