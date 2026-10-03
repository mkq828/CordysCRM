package cn.cordys.crm.ai.callreview.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.callreview.domain.AiCallReviewConfig;
import cn.cordys.crm.ai.callreview.dto.response.CallReviewConfigResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通话复盘回调配置：appKey/secretKey 生成与重置、字段映射保存、回调地址拼装。
 * 密钥第一版明文存储（与系统其余第三方配置一致），不另造加密。
 */
@Service
public class CallReviewConfigService {

    @Resource
    private BaseMapper<AiCallReviewConfig> configMapper;

    /** 读取或首次创建租户回调配置（首次自动生成 appKey/secretKey） */
    public AiCallReviewConfig getOrCreate(String orgId, String userId) {
        AiCallReviewConfig config = find(orgId);
        if (config == null) {
            long now = System.currentTimeMillis();
            config = new AiCallReviewConfig();
            config.setId(IDGenerator.nextStr());
            config.setOrganizationId(orgId);
            config.setAppKey(CodingUtils.generateAK());
            config.setSecretKey(CodingUtils.generateSecretKey());
            config.setFieldMapping("{}");
            config.setEnable(Boolean.TRUE);
            config.setCreateUser(userId);
            config.setUpdateUser(userId);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            configMapper.insert(config);
        }
        return config;
    }

    /** 保存字段映射与启用开关 */
    public AiCallReviewConfig save(String orgId, String userId, Map<String, String> fieldMapping, Boolean enable) {
        AiCallReviewConfig config = getOrCreate(orgId, userId);
        config.setFieldMapping(fieldMapping == null ? "{}" : JSON.toJSONString(fieldMapping));
        if (enable != null) {
            config.setEnable(enable);
        }
        config.setUpdateUser(userId);
        config.setUpdateTime(System.currentTimeMillis());
        configMapper.updateById(config);
        return config;
    }

    /** 重置回调密钥（更换 appKey/secretKey，旧回调地址随即失效） */
    public AiCallReviewConfig resetKey(String orgId, String userId) {
        AiCallReviewConfig config = getOrCreate(orgId, userId);
        config.setAppKey(CodingUtils.generateAK());
        config.setSecretKey(CodingUtils.generateSecretKey());
        config.setUpdateUser(userId);
        config.setUpdateTime(System.currentTimeMillis());
        configMapper.updateById(config);
        return config;
    }

    /** 按 appKey 查配置（回调鉴权用） */
    public AiCallReviewConfig findByAppKey(String appKey) {
        List<AiCallReviewConfig> list = configMapper.selectListByLambda(new LambdaQueryWrapper<AiCallReviewConfig>()
                .eq(AiCallReviewConfig::getAppKey, appKey));
        return list.isEmpty() ? null : list.getFirst();
    }

    public AiCallReviewConfig find(String orgId) {
        List<AiCallReviewConfig> list = configMapper.selectListByLambda(new LambdaQueryWrapper<AiCallReviewConfig>()
                .eq(AiCallReviewConfig::getOrganizationId, orgId));
        return list.isEmpty() ? null : list.getFirst();
    }

    public CallReviewConfigResponse toResponse(AiCallReviewConfig config, String baseUrl) {
        CallReviewConfigResponse response = new CallReviewConfigResponse();
        response.setAppKey(config.getAppKey());
        response.setSecretKey(config.getSecretKey());
        response.setCallbackUrl(baseUrl + "/open/call-review/callback/" + config.getAppKey());
        response.setFieldMapping(parseMapping(config.getFieldMapping()));
        response.setEnable(config.getEnable());
        return response;
    }

    private Map<String, String> parseMapping(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> mapping = JSON.parseObject(json, new TypeReference<Map<String, String>>() { });
            return mapping == null ? new LinkedHashMap<>() : mapping;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
