package cn.cordys.crm.ai.dmprofile.dto;

import lombok.Data;

/**
 * 单条私信消息：sender = me/other。
 */
@Data
public class DmRawMessage {

    private String sender;

    private String content;
}
