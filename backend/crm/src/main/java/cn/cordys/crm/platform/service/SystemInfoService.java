package cn.cordys.crm.platform.service;

import cn.cordys.crm.platform.dto.request.SystemInfoRequest;
import cn.cordys.crm.platform.dto.response.SystemInfoResponse;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.service.SystemService;
import cn.cordys.crm.system.utils.CopyrightUtils;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 平台系统信息配置（关于弹窗）：运营方名称。
 * Logo 复用「业务设置-平台主页面配置」里的平台 Logo（ui.logoPlatform），此处不重复维护。
 * 运营方名称存 sys_parameter 全局参数 platform.operator，未配置时回退代码默认值。
 */
@Service
public class SystemInfoService {

    private static final String KEY_OPERATOR = "platform.operator";

    @Resource
    private BaseMapper<Parameter> parameterMapper;

    @Resource
    private SystemService systemService;

    public SystemInfoResponse get() {
        SystemInfoResponse response = new SystemInfoResponse();
        String operator = getParam(KEY_OPERATOR);
        response.setOperator(StringUtils.isBlank(operator) ? CopyrightUtils.getOperator() : operator);
        return response;
    }

    public void update(SystemInfoRequest request) {
        if (StringUtils.isNotBlank(request.getOperator())) {
            setParam(KEY_OPERATOR, request.getOperator());
        } else {
            deleteParam(KEY_OPERATOR);
        }
        systemService.clearVersionCache();
    }

    private String getParam(String key) {
        Parameter parameter = parameterMapper.selectByPrimaryKey(key);
        return parameter == null ? null : parameter.getParamValue();
    }

    private void setParam(String key, String value) {
        parameterMapper.deleteByPrimaryKey(key);
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        parameter.setType("text");
        parameterMapper.insert(parameter);
    }

    private void deleteParam(String key) {
        parameterMapper.deleteByPrimaryKey(key);
    }
}
