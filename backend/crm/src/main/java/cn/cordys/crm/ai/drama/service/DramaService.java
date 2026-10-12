package cn.cordys.crm.ai.drama.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.ai.constant.AiQuotaConstant;
import cn.cordys.crm.ai.drama.domain.AiDramaAsset;
import cn.cordys.crm.ai.drama.domain.AiDramaEpisode;
import cn.cordys.crm.ai.drama.domain.AiDramaFragment;
import cn.cordys.crm.ai.drama.domain.AiDramaProject;
import cn.cordys.crm.ai.drama.domain.AiDramaScript;
import cn.cordys.crm.ai.drama.dto.request.DramaAssetCreateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaAssetUpdateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaEpisodeUpdateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaProjectCreateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaProjectUpdateRequest;
import cn.cordys.crm.ai.drama.dto.request.DramaScriptUpdateRequest;
import cn.cordys.crm.ai.drama.dto.response.DramaAssetResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaEpisodeResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaFragmentResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaProjectResponse;
import cn.cordys.crm.ai.drama.dto.response.DramaScriptResponse;
import cn.cordys.crm.ai.video.service.VideoGenerateService;
import cn.cordys.crm.ai.video.tts.TtsVoice;
import cn.cordys.crm.ai.video.dto.response.VideoModelsResponse;
import cn.cordys.crm.system.service.EditionService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 漫剧：项目/剧本/分集/分镜/资产的 CRUD 与门控。
 * 生成类逻辑（剧本摘要、分镜规划、出图/出视频、合成）在 {@link DramaGenerateService}。
 */
@Slf4j
@Service
public class DramaService {

    @Resource
    private BaseMapper<AiDramaProject> projectMapper;
    @Resource
    private BaseMapper<AiDramaScript> scriptMapper;
    @Resource
    private BaseMapper<AiDramaEpisode> episodeMapper;
    @Resource
    private BaseMapper<AiDramaFragment> fragmentMapper;
    @Resource
    private BaseMapper<AiDramaAsset> assetMapper;
    @Resource
    private EditionService editionService;
    @Resource
    private VideoGenerateService videoGenerateService;

    // ==================== 项目 ====================

    public List<DramaProjectResponse> listProjects(String orgId) {
        List<AiDramaProject> projects = projectMapper.selectListByLambda(new LambdaQueryWrapper<AiDramaProject>()
                .eq(AiDramaProject::getOrganizationId, orgId)
                .orderByDesc(AiDramaProject::getCreateTime));
        List<DramaProjectResponse> result = new ArrayList<>(projects.size());
        for (AiDramaProject project : projects) {
            result.add(toProjectResponse(project, orgId));
        }
        return result;
    }

    public DramaProjectResponse createProject(DramaProjectCreateRequest request, String orgId, String userId) {
        long now = System.currentTimeMillis();
        AiDramaProject project = new AiDramaProject();
        project.setId(IDGenerator.nextStr());
        project.setOrganizationId(orgId);
        project.setTitle(request.getTitle().trim());
        // description 仅存一句话简介（512 字内）；完整故事落在 ai_drama_script.source_text（MEDIUMTEXT）
        project.setDescription(StringUtils.truncate(StringUtils.trimToNull(request.getSource()), 512));
        project.setWorkflow(StringUtils.defaultIfBlank(request.getWorkflow(), "script"));
        project.setCreateUser(userId);
        project.setUpdateUser(userId);
        project.setCreateTime(now);
        project.setUpdateTime(now);
        projectMapper.insert(project);

        // 剧本与项目一对一，随项目创建空剧本，供后续 AI 生剧本写入
        AiDramaScript script = new AiDramaScript();
        script.setId(IDGenerator.nextStr());
        script.setOrganizationId(orgId);
        script.setProjectId(project.getId());
        script.setName(project.getTitle());
        script.setSourceText(StringUtils.defaultIfBlank(request.getSource(), request.getTitle()));
        script.setParamsJson(initialScriptParams(request.getImageStyleId(), request.getEpisodeCount()));
        script.setCreateUser(userId);
        script.setUpdateUser(userId);
        script.setCreateTime(now);
        script.setUpdateTime(now);
        scriptMapper.insert(script);

        return toProjectResponse(project, orgId);
    }

    public DramaProjectResponse getProject(String orgId, String id) {
        AiDramaProject project = requireProject(orgId, id);
        DramaProjectResponse response = toProjectResponse(project, orgId);
        response.setScript(getScript(orgId, id));
        return response;
    }

    public DramaProjectResponse updateProject(String orgId, String id, DramaProjectUpdateRequest request) {
        AiDramaProject project = requireProject(orgId, id);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            project.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(StringUtils.trimToNull(request.getDescription()));
        }
        if (request.getParamsJson() != null) {
            project.setParamsJson(StringUtils.trimToNull(request.getParamsJson()));
        }
        project.setUpdateTime(System.currentTimeMillis());
        projectMapper.updateById(project);
        return toProjectResponse(project, orgId);
    }

    public void deleteProject(String orgId, String id) {
        AiDramaProject project = requireProject(orgId, id);
        // 级联删除：分镜 → 分集 → 资产 → 剧本 → 项目
        List<AiDramaEpisode> episodes = episodes(orgId, id);
        for (AiDramaEpisode episode : episodes) {
            fragmentMapper.deleteByLambda(new LambdaQueryWrapper<AiDramaFragment>()
                    .eq(AiDramaFragment::getEpisodeId, episode.getId()));
        }
        episodeMapper.deleteByLambda(new LambdaQueryWrapper<AiDramaEpisode>()
                .eq(AiDramaEpisode::getProjectId, id));
        assetMapper.deleteByLambda(new LambdaQueryWrapper<AiDramaAsset>()
                .eq(AiDramaAsset::getProjectId, id));
        scriptMapper.deleteByLambda(new LambdaQueryWrapper<AiDramaScript>()
                .eq(AiDramaScript::getProjectId, id));
        projectMapper.deleteByPrimaryKey(project.getId());
    }

    // ==================== 剧本 ====================

    public DramaScriptResponse getScript(String orgId, String projectId) {
        AiDramaScript script = findScript(orgId, projectId);
        return script == null ? null : toScriptResponse(script);
    }

    public DramaScriptResponse updateScript(String orgId, String projectId, DramaScriptUpdateRequest request) {
        AiDramaScript script = requireScript(orgId, projectId);
        if (request.getSource() != null) {
            script.setSourceText(StringUtils.trimToNull(request.getSource()));
        }
        if (request.getSummaryJson() != null) {
            script.setSummaryJson(StringUtils.trimToNull(request.getSummaryJson()));
        }
        if (request.getEpisodeContentJson() != null) {
            script.setEpisodeContentJson(StringUtils.trimToNull(request.getEpisodeContentJson()));
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            script.setName(request.getName().trim());
        }
        if (request.getImageStyleId() != null) {
            script.setParamsJson(putImageStyleId(script.getParamsJson(), request.getImageStyleId()));
        }
        script.setUpdateTime(System.currentTimeMillis());
        scriptMapper.updateById(script);
        return toScriptResponse(script);
    }

    // ==================== 分集 ====================

    public List<DramaEpisodeResponse> listEpisodes(String orgId, String projectId) {
        List<AiDramaEpisode> episodes = episodes(orgId, projectId);
        List<DramaEpisodeResponse> result = new ArrayList<>(episodes.size());
        for (AiDramaEpisode episode : episodes) {
            result.add(toEpisodeResponse(episode, fragments(orgId, episode.getId())));
        }
        return result;
    }

    public DramaEpisodeResponse getEpisode(String orgId, String id) {
        AiDramaEpisode episode = requireEpisode(orgId, id);
        return toEpisodeResponse(episode, fragments(orgId, id));
    }

    public DramaEpisodeResponse updateEpisode(String orgId, String id, DramaEpisodeUpdateRequest request) {
        AiDramaEpisode episode = requireEpisode(orgId, id);
        if (request.getName() != null && !request.getName().isBlank()) {
            episode.setName(request.getName().trim());
        }
        if (request.getParamsJson() != null) {
            episode.setParamsJson(StringUtils.trimToNull(request.getParamsJson()));
        }
        episode.setUpdateTime(System.currentTimeMillis());
        episodeMapper.updateById(episode);
        return toEpisodeResponse(episode, fragments(orgId, id));
    }

    // ==================== 资产 ====================

    public List<DramaAssetResponse> listAssets(String orgId, String projectId) {
        LambdaQueryWrapper<AiDramaAsset> assetWrapper = new LambdaQueryWrapper<AiDramaAsset>()
                .eq(AiDramaAsset::getProjectId, projectId);
        assetWrapper.orderByAsc(AiDramaAsset::getCreateTime);
        List<AiDramaAsset> assets = assetMapper.selectListByLambda(assetWrapper);
        List<DramaAssetResponse> result = new ArrayList<>(assets.size());
        for (AiDramaAsset asset : assets) {
            result.add(toAssetResponse(asset));
        }
        return result;
    }

    /** 全局资产库：按租户列出全部项目资产（跨项目），新建在前 */
    public List<DramaAssetResponse> listAllAssets(String orgId) {
        LambdaQueryWrapper<AiDramaAsset> assetWrapper = new LambdaQueryWrapper<AiDramaAsset>()
                .eq(AiDramaAsset::getOrganizationId, orgId);
        assetWrapper.orderByDesc(AiDramaAsset::getCreateTime);
        List<AiDramaAsset> assets = assetMapper.selectListByLambda(assetWrapper);
        List<DramaAssetResponse> result = new ArrayList<>(assets.size());
        for (AiDramaAsset asset : assets) {
            result.add(toAssetResponse(asset));
        }
        return result;
    }

    public DramaAssetResponse createAsset(String orgId, DramaAssetCreateRequest request, String userId) {
        long now = System.currentTimeMillis();
        AiDramaAsset asset = new AiDramaAsset();
        asset.setId(IDGenerator.nextStr());
        asset.setOrganizationId(orgId);
        asset.setProjectId(request.getProjectId());
        asset.setType(StringUtils.defaultIfBlank(request.getType(), "character"));
        asset.setAssetType(StringUtils.trimToNull(request.getAssetType()));
        asset.setName(StringUtils.defaultIfBlank(request.getName(), "未命名"));
        asset.setParamsJson(StringUtils.trimToNull(request.getParamsJson()));
        asset.setCreateUser(userId);
        asset.setUpdateUser(userId);
        asset.setCreateTime(now);
        asset.setUpdateTime(now);
        assetMapper.insert(asset);
        return toAssetResponse(asset);
    }

    public DramaAssetResponse updateAsset(String orgId, String id, DramaAssetUpdateRequest request) {
        log.info("[drama] updateAsset id={} voice='{}' name='{}' assetType='{}' paramsJsonLen={}",
                id, request.getVoice(), request.getName(), request.getAssetType(),
                request.getParamsJson() == null ? -1 : request.getParamsJson().length());
        AiDramaAsset asset = requireAsset(orgId, id);
        if (request.getName() != null && !request.getName().isBlank()) {
            asset.setName(request.getName().trim());
        }
        if (request.getVoice() != null) {
            asset.setVoice(StringUtils.trimToNull(request.getVoice()));
        }
        if (request.getAssetType() != null) {
            asset.setAssetType(StringUtils.trimToNull(request.getAssetType()));
        }
        if (request.getParamsJson() != null) {
            asset.setParamsJson(StringUtils.trimToNull(request.getParamsJson()));
        }
        asset.setUpdateTime(System.currentTimeMillis());
        assetMapper.updateById(asset);
        return toAssetResponse(asset);
    }

    public void deleteAsset(String orgId, String id) {
        requireAsset(orgId, id);
        assetMapper.deleteByPrimaryKey(id);
    }

    // ==================== 门控 / 模型 / 音色 ====================

    public boolean available(String orgId) {
        return editionService.hasFeature(orgId, AiQuotaConstant.AI_DRAMA);
    }

    /** 图片/视频模型列表，复用短视频同一条「模型设置」数据源 */
    public VideoModelsResponse models(String orgId) {
        return videoGenerateService.models(orgId);
    }

    /** 配音音色列表（含本租户克隆音色），复用短视频同一条 TTS 链 */
    public List<TtsVoice> voices(String orgId) {
        return videoGenerateService.voices(orgId);
    }

    // ==================== 内部查询与转换 ====================

    private AiDramaProject requireProject(String orgId, String id) {
        AiDramaProject project = projectMapper.selectByPrimaryKey(id);
        if (project == null || !orgId.equals(project.getOrganizationId())) {
            throw new GenericException("漫剧项目不存在");
        }
        return project;
    }

    private AiDramaScript findScript(String orgId, String projectId) {
        return scriptMapper.selectOne(newScript(projectId, orgId));
    }

    private AiDramaScript requireScript(String orgId, String projectId) {
        AiDramaScript script = findScript(orgId, projectId);
        if (script == null) {
            throw new GenericException("漫剧剧本不存在");
        }
        return script;
    }

    private AiDramaEpisode requireEpisode(String orgId, String id) {
        AiDramaEpisode episode = episodeMapper.selectByPrimaryKey(id);
        if (episode == null || !orgId.equals(episode.getOrganizationId())) {
            throw new GenericException("漫剧分集不存在");
        }
        return episode;
    }

    private AiDramaAsset requireAsset(String orgId, String id) {
        AiDramaAsset asset = assetMapper.selectByPrimaryKey(id);
        if (asset == null || !orgId.equals(asset.getOrganizationId())) {
            throw new GenericException("漫剧资产不存在");
        }
        return asset;
    }

    private List<AiDramaEpisode> episodes(String orgId, String projectId) {
        LambdaQueryWrapper<AiDramaEpisode> wrapper = new LambdaQueryWrapper<AiDramaEpisode>()
                .eq(AiDramaEpisode::getProjectId, projectId);
        wrapper.orderByAsc(AiDramaEpisode::getSortOrder);
        return episodeMapper.selectListByLambda(wrapper);
    }

    private List<AiDramaFragment> fragments(String orgId, String episodeId) {
        LambdaQueryWrapper<AiDramaFragment> wrapper = new LambdaQueryWrapper<AiDramaFragment>()
                .eq(AiDramaFragment::getEpisodeId, episodeId);
        wrapper.orderByAsc(AiDramaFragment::getSortOrder);
        return fragmentMapper.selectListByLambda(wrapper);
    }

    private AiDramaScript newScript(String projectId, String orgId) {
        AiDramaScript criteria = new AiDramaScript();
        criteria.setProjectId(projectId);
        criteria.setOrganizationId(orgId);
        return criteria;
    }

    /** 建项目时的剧本初始参数：画面风格 + 目标集数（集数是脚本工作流的解析源，必须落库） */
    private String initialScriptParams(String imageStyleId, Integer episodeCount) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (StringUtils.isNotBlank(imageStyleId)) {
            sb.append("\"image_style_id\":\"").append(imageStyleId.trim()).append('"');
            first = false;
        }
        if (episodeCount != null && episodeCount > 0) {
            if (!first) {
                sb.append(',');
            }
            sb.append("\"episode_count\":").append(episodeCount);
        }
        sb.append('}');
        return sb.toString();
    }

    /** 覆盖 paramsJson 里的 image_style_id（保留 episode_count 等其他键） */
    private String putImageStyleId(String paramsJson, String imageStyleId) {
        JsonNode node = null;
        try {
            node = JSON.MAPPER.readTree(StringUtils.defaultIfBlank(paramsJson, "{}"));
        } catch (Exception ignored) {
            node = JSON.MAPPER.createObjectNode();
        }
        ObjectNode object = node != null && node.isObject() ? (ObjectNode) node : JSON.MAPPER.createObjectNode();
        if (StringUtils.isBlank(imageStyleId)) {
            object.remove("image_style_id");
        } else {
            object.put("image_style_id", imageStyleId.trim());
        }
        return object.isEmpty() ? null : object.toString();
    }

    /** 解析 paramsJson 为对象节点，空/非法返回空对象节点 */
    private ObjectNode parseParams(String paramsJson) {
        try {
            JsonNode node = JSON.MAPPER.readTree(StringUtils.defaultIfBlank(paramsJson, "{}"));
            return node != null && node.isObject() ? (ObjectNode) node : JSON.MAPPER.createObjectNode();
        } catch (Exception ignored) {
            return JSON.MAPPER.createObjectNode();
        }
    }

    private DramaProjectResponse toProjectResponse(AiDramaProject project, String orgId) {
        DramaProjectResponse response = new DramaProjectResponse();
        response.setId(project.getId());
        response.setTitle(project.getTitle());
        response.setDescription(project.getDescription());
        response.setWorkflow(project.getWorkflow());
        response.setCoverAttachmentId(project.getCoverAttachmentId());
        response.setParamsJson(project.getParamsJson());
        response.setEpisodeCount(episodes(orgId, project.getId()).size());
        response.setAssetCount(assetsCount(project.getId()));
        response.setHasScript(findScript(orgId, project.getId()) != null);
        response.setCreateTime(project.getCreateTime());
        response.setUpdateTime(project.getUpdateTime());
        return response;
    }

    private int assetsCount(String projectId) {
        AiDramaAsset criteria = new AiDramaAsset();
        criteria.setProjectId(projectId);
        Long count = assetMapper.countByExample(criteria);
        return count == null ? 0 : count.intValue();
    }

    public DramaScriptResponse toScriptResponse(AiDramaScript script) {
        DramaScriptResponse response = new DramaScriptResponse();
        response.setId(script.getId());
        response.setProjectId(script.getProjectId());
        response.setName(script.getName());
        response.setSourceText(script.getSourceText());
        response.setSummaryJson(script.getSummaryJson());
        response.setEpisodeContentJson(script.getEpisodeContentJson());
        response.setSummaryStatus(script.getSummaryStatus());
        response.setParamsJson(script.getParamsJson());
        return response;
    }

    private DramaEpisodeResponse toEpisodeResponse(AiDramaEpisode episode, List<AiDramaFragment> fragments) {
        DramaEpisodeResponse response = new DramaEpisodeResponse();
        response.setId(episode.getId());
        response.setProjectId(episode.getProjectId());
        response.setName(episode.getName());
        response.setSortOrder(episode.getSortOrder());
        response.setStatus(episode.getStatus());
        response.setVideoAttachmentId(episode.getVideoAttachmentId());
        JsonNode params = parseParams(episode.getParamsJson());
        response.setNarrationStatus(params.path("narration_status").asText(null));
        response.setVideoStatus(params.path("video_status").asText(null));
        response.setVideoError(params.path("video_error").asText(null));
        response.setComposeStatus(params.path("compose_status").asText(null));
        response.setComposeError(params.path("compose_error").asText(null));
        List<DramaFragmentResponse> fragmentResponses = new ArrayList<>(fragments.size());
        for (AiDramaFragment fragment : fragments) {
            fragmentResponses.add(toFragmentResponse(fragment));
        }
        response.setFragments(fragmentResponses);
        return response;
    }

    private DramaFragmentResponse toFragmentResponse(AiDramaFragment fragment) {
        DramaFragmentResponse response = new DramaFragmentResponse();
        response.setId(fragment.getId());
        response.setEpisodeId(fragment.getEpisodeId());
        response.setSortOrder(fragment.getSortOrder());
        response.setContent(fragment.getContent());
        response.setNarration(fragment.getNarration());
        response.setNarrationAttachmentId(fragment.getNarrationAttachmentId());
        response.setVoice(fragment.getVoice());
        response.setDurationSec(fragment.getDurationSec());
        response.setCoverAttachmentId(fragment.getCoverAttachmentId());
        response.setVideoAttachmentId(fragment.getVideoAttachmentId());
        response.setAssetIdsJson(fragment.getAssetIdsJson());
        response.setStatus(fragment.getStatus());
        return response;
    }

    public DramaAssetResponse toAssetResponse(AiDramaAsset asset) {
        DramaAssetResponse response = new DramaAssetResponse();
        response.setId(asset.getId());
        response.setProjectId(asset.getProjectId());
        response.setType(asset.getType());
        response.setAssetType(asset.getAssetType());
        response.setName(asset.getName());
        response.setVoice(asset.getVoice());
        response.setCoverAttachmentId(asset.getCoverAttachmentId());
        response.setParamsJson(asset.getParamsJson());
        response.setStatus(asset.getStatus());
        return response;
    }
}
