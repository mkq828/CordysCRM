package cn.cordys.crm.platform.service;

import cn.cordys.crm.platform.dto.request.PlatformConfigRequest;
import cn.cordys.crm.platform.dto.response.PlatformConfigResponse;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 平台收款主体配置服务：收款公司/开票抬头/默认税率（sys_parameter 全局参数）。
 * 收款账户（对公/支付宝/微信/线下）已独立为 platform_bank_account 表，见 PlatformBankAccountService。
 */
@Service
public class PlatformConfigService {

    private static final String KEY_COMPANY_NAME = "platform.companyName";
    private static final String KEY_INVOICE_TITLE = "platform.invoiceTitle";
    private static final String KEY_TAX_RATE = "platform.taxRate";

    @Resource
    private BaseMapper<Parameter> parameterMapper;

    /**
     * 读取平台收款主体配置
     */
    public PlatformConfigResponse get() {
        PlatformConfigResponse response = new PlatformConfigResponse();
        response.setCompanyName(getParam(KEY_COMPANY_NAME));
        response.setInvoiceTitle(getParam(KEY_INVOICE_TITLE));
        response.setTaxRate(getParam(KEY_TAX_RATE));
        return response;
    }

    /**
     * 保存平台收款主体配置
     */
    public void update(PlatformConfigRequest request) {
        setParam(KEY_COMPANY_NAME, request.getCompanyName());
        setParam(KEY_INVOICE_TITLE, request.getInvoiceTitle());
        setParam(KEY_TAX_RATE, request.getTaxRate());
    }

    private String getParam(String key) {
        Parameter parameter = parameterMapper.selectByPrimaryKey(key);
        return parameter == null ? null : parameter.getParamValue();
    }

    private void setParam(String key, String value) {
        parameterMapper.deleteByPrimaryKey(key);
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue(value == null ? "" : value);
        parameter.setType("text");
        parameterMapper.insert(parameter);
    }
}
