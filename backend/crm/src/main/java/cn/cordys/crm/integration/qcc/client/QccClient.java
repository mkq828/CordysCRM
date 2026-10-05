package cn.cordys.crm.integration.qcc.client;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.common.request.QccThirdConfigRequest;
import cn.cordys.crm.integration.common.utils.HttpClientUtils;
import cn.cordys.crm.integration.qcc.constant.QccApiPaths;
import cn.cordys.crm.integration.qcc.dto.EnterpriseInfo;
import cn.cordys.crm.integration.qcc.dto.InfoData;
import cn.cordys.crm.integration.qcc.dto.NameInfo;
import cn.cordys.crm.integration.qcc.dto.QccEnterpriseInfo;
import cn.cordys.crm.integration.qcc.dto.QccFuzzyQueryInfo;
import cn.cordys.crm.integration.qcc.response.QccBaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企查查开放平台客户端：模糊搜索 + 企业详情核验。
 * 鉴权方式：Token 头 = MD5(AccessKey + 秒级时间戳 + SecretKey) 转大写，配合 Timespan 头；
 * URL 参数 key=AccessKey、searchKey=检索关键字。
 */
@Component
@Slf4j
public class QccClient {

    /** 企查查接口成功（有数据） */
    private static final String STATUS_SUCCESS = "200";

    /** 企查查接口成功但无数据 */
    private static final String STATUS_NO_DATA = "201";

    /**
     * 模糊搜索企业名单：按关键字返回企业名称列表（不含详情，便宜接口）。
     *
     * @param config  租户企查查配置
     * @param keyword 搜索关键字
     * @return 企业名称列表，无数据时返回空列表
     */
    public List<String> fuzzySearch(QccThirdConfigRequest config, String keyword) {
        String body = get(config, QccApiPaths.FUZZY_SEARCH_API, keyword);
        QccFuzzyQueryInfo resp = parse(body, QccFuzzyQueryInfo.class, keyword);
        if (STATUS_NO_DATA.equals(resp.getStatus()) || resp.getResult() == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (NameInfo info : resp.getResult()) {
            if (info != null && StringUtils.isNotBlank(info.getName())) {
                names.add(info.getName().trim());
            }
        }
        return names;
    }

    /**
     * 企业详情核验：按企业全称拉取完整工商信息（贵接口，命中缓存后不应再调）。
     *
     * @param config 租户企查查配置
     * @param name   企业全称
     * @return 企业详情；查无此企业时返回 null
     */
    public InfoData verify(QccThirdConfigRequest config, String name) {
        String body = get(config, QccApiPaths.ENTERPRISE_INFO_VERIFY_API, name);
        QccEnterpriseInfo resp = parse(body, QccEnterpriseInfo.class, name);
        if (STATUS_NO_DATA.equals(resp.getStatus())) {
            return null;
        }
        EnterpriseInfo result = resp.getResult();
        if (result == null || result.getData() == null) {
            return null;
        }
        if (result.getVerifyResult() != null && result.getVerifyResult() == 0) {
            return null;
        }
        return result.getData();
    }

    /** 发起带企查查签名的 GET 请求，返回响应体字符串 */
    private String get(QccThirdConfigRequest config, String path, String keyword) {
        long time = System.currentTimeMillis() / 1000;
        String token = CodingUtils.md5(config.getQccAccessKey() + time + config.getQccSecretKey()).toUpperCase();
        Map<String, String> headers = new HashMap<>();
        headers.put("Token", token);
        headers.put("Timespan", String.valueOf(time));

        String url = HttpClientUtils.urlTransfer(config.getQccAddress().concat(path), config.getQccAccessKey(), keyword);
        try {
            return HttpClientUtils.sendGetRequest(url, headers);
        } catch (Exception e) {
            log.error("调用企查查接口失败, path={}, keyword={}", path, keyword, e);
            throw new GenericException("调用企查查失败，请检查配置或稍后重试");
        }
    }

    /** 解析企查查响应，非成功状态抛可读错误 */
    private <T extends QccBaseResponse> T parse(String body, Class<T> clazz, String keyword) {
        T resp;
        try {
            resp = JSON.parseObject(body, clazz);
        } catch (Exception e) {
            log.error("解析企查查响应失败, body={}", body, e);
            throw new GenericException("企查查返回数据异常，请稍后重试");
        }
        if (STATUS_SUCCESS.equals(resp.getStatus()) || STATUS_NO_DATA.equals(resp.getStatus())) {
            return resp;
        }
        log.warn("企查查接口返回非成功状态, status={}, message={}, keyword={}",
                resp.getStatus(), resp.getMessage(), keyword);
        throw new GenericException(StringUtils.isBlank(resp.getMessage()) ? "企查查查询失败，请检查 API Key 或额度" : resp.getMessage());
    }
}
