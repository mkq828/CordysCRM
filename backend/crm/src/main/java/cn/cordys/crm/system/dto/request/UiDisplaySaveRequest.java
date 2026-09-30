package cn.cordys.crm.system.dto.request;

import lombok.Data;

/**
 * 界面设置保存项（对应前端 savePageConfig 的 request 数组元素）。
 */
@Data
public class UiDisplaySaveRequest {

    private String paramKey;

    private String paramValue;

    private String type;

    private String fileName;

    /** 是否为默认值（文件被清空，回默认） */
    private Boolean original;

    /** 是否上传了新文件 */
    private Boolean hasFile;

    private String organizationId;
}
