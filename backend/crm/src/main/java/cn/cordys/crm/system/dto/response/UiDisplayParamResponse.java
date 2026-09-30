package cn.cordys.crm.system.dto.response;

import lombok.Data;

/**
 * 界面设置参数返回项（对应前端 getPageConfig 返回）。
 */
@Data
public class UiDisplayParamResponse {

    private String paramKey;

    private String paramValue;

    private String type;

    private String fileName;
}
