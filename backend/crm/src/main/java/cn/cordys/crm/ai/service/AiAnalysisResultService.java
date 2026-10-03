package cn.cordys.crm.ai.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.domain.AiAnalysisResult;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 分析结果沉淀：生成型 AI 能力把结构化结论按业务对象写入 {@link AiAnalysisResult}，
 * 客户画像 / 数据大屏只读回读，本身不调模型、不计量。
 */
@Service
public class AiAnalysisResultService {

    @Resource
    private BaseMapper<AiAnalysisResult> analysisResultMapper;

    /** 落一条分析结果（同一业务对象允许多条历史，画像取最新一条） */
    public void save(String organizationId, String bizType, String bizId, String featureCode,
                     String title, String resultJson, String modelCode, String userId) {
        long now = System.currentTimeMillis();
        AiAnalysisResult result = new AiAnalysisResult();
        result.setId(IDGenerator.nextStr());
        result.setOrganizationId(organizationId);
        result.setBizType(bizType);
        result.setBizId(bizId);
        result.setFeatureCode(featureCode);
        result.setTitle(title);
        result.setResultJson(resultJson);
        result.setModelCode(modelCode);
        result.setCreateUser(userId);
        result.setUpdateUser(userId);
        result.setCreateTime(now);
        result.setUpdateTime(now);
        analysisResultMapper.insert(result);
    }

    /** 取某业务对象最新一条分析结果（无则返回 null） */
    public AiAnalysisResult latest(String organizationId, String bizType, String bizId) {
        List<AiAnalysisResult> list = analysisResultMapper.selectListByLambda(new LambdaQueryWrapper<AiAnalysisResult>()
                .eq(AiAnalysisResult::getOrganizationId, organizationId)
                .eq(AiAnalysisResult::getBizType, bizType)
                .eq(AiAnalysisResult::getBizId, bizId)
                .orderByDesc(AiAnalysisResult::getCreateTime));
        return list.isEmpty() ? null : list.getFirst();
    }
}
