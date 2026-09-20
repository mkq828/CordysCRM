package cn.cordys.crm.suggestion.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.suggestion.domain.Suggestion;
import cn.cordys.crm.suggestion.domain.SuggestionComment;
import cn.cordys.crm.suggestion.domain.SuggestionVote;
import cn.cordys.crm.suggestion.dto.request.SuggestionAddRequest;
import cn.cordys.crm.suggestion.dto.request.SuggestionCommentAddRequest;
import cn.cordys.crm.suggestion.dto.request.SuggestionPageRequest;
import cn.cordys.crm.suggestion.dto.request.SuggestionStatusRequest;
import cn.cordys.crm.suggestion.dto.response.SuggestionCommentDTO;
import cn.cordys.crm.suggestion.dto.response.SuggestionDTO;
import cn.cordys.crm.suggestion.dto.response.SuggestionVoteResponse;
import cn.cordys.crm.suggestion.enums.SuggestionStatus;
import cn.cordys.crm.suggestion.mapper.ExtSuggestionMapper;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 需求反馈服务（平台全局，不按租户隔离）。
 */
@Service
public class SuggestionService {

    @Resource
    private BaseMapper<Suggestion> suggestionMapper;
    @Resource
    private BaseMapper<SuggestionVote> voteMapper;
    @Resource
    private BaseMapper<SuggestionComment> commentMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private ExtSuggestionMapper extSuggestionMapper;
    @Resource
    private AttachmentService attachmentService;

    public List<SuggestionDTO> page(SuggestionPageRequest request) {
        List<SuggestionDTO> list = extSuggestionMapper.page(request);
        fillVoted(list);
        return list;
    }

    public SuggestionDTO detail(String id) {
        SuggestionDTO dto = extSuggestionMapper.detail(id);
        if (dto == null) {
            throw new GenericException(Translator.get("suggestion.not_found"));
        }
        dto.setImageList(loadImages(dto.getImageIds()));
        dto.setVoted(hasVoted(id, SessionUtils.getUserId()));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public String add(SuggestionAddRequest request, String userId) {
        String organizationId = OrganizationContext.getOrganizationId();
        Suggestion suggestion = new Suggestion();
        suggestion.setId(IDGenerator.nextStr());
        suggestion.setTitle(request.getTitle());
        suggestion.setContent(request.getContent());
        suggestion.setOrganizationId(organizationId);
        suggestion.setUserId(userId);
        suggestion.setStatus(SuggestionStatus.PENDING.name());
        suggestion.setVoteCount(0);
        suggestion.setCreateTime(System.currentTimeMillis());
        suggestion.setUpdateTime(System.currentTimeMillis());
        suggestion.setCreateUser(userId);
        suggestion.setUpdateUser(userId);
        if (!CollectionUtils.isEmpty(request.getImageIds())) {
            suggestion.setImageIds(JSON.toJSONString(request.getImageIds()));
        }
        suggestionMapper.insert(suggestion);
        if (!CollectionUtils.isEmpty(request.getImageIds())) {
            attachmentService.processTemp(new UploadTransferRequest(organizationId, suggestion.getId(), userId, request.getImageIds()));
        }
        return suggestion.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public SuggestionVoteResponse vote(String suggestionId, String userId) {
        Suggestion suggestion = suggestionMapper.selectByPrimaryKey(suggestionId);
        if (suggestion == null) {
            throw new GenericException(Translator.get("suggestion.not_found"));
        }
        LambdaQueryWrapper<SuggestionVote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SuggestionVote::getSuggestionId, suggestionId).eq(SuggestionVote::getUserId, userId);
        boolean voted;
        if (CollectionUtils.isEmpty(voteMapper.selectListByLambda(queryWrapper))) {
            SuggestionVote vote = new SuggestionVote();
            vote.setId(IDGenerator.nextStr());
            vote.setSuggestionId(suggestionId);
            vote.setUserId(userId);
            vote.setCreateTime(System.currentTimeMillis());
            vote.setUpdateTime(System.currentTimeMillis());
            vote.setCreateUser(userId);
            vote.setUpdateUser(userId);
            voteMapper.insert(vote);
            extSuggestionMapper.updateVoteCount(suggestionId, 1);
            voted = true;
        } else {
            voteMapper.deleteByLambda(queryWrapper);
            extSuggestionMapper.updateVoteCount(suggestionId, -1);
            voted = false;
        }
        int currentCount = suggestion.getVoteCount() == null ? 0 : suggestion.getVoteCount();
        SuggestionVoteResponse response = new SuggestionVoteResponse();
        response.setVoteCount(currentCount + (voted ? 1 : -1));
        response.setVoted(voted);
        return response;
    }

    public List<SuggestionCommentDTO> listComments(String suggestionId) {
        return extSuggestionMapper.listComments(suggestionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addComment(SuggestionCommentAddRequest request, String userId) {
        SuggestionComment comment = new SuggestionComment();
        comment.setId(IDGenerator.nextStr());
        comment.setSuggestionId(request.getSuggestionId());
        comment.setContent(request.getContent());
        comment.setReplyCommentId(request.getReplyCommentId());
        comment.setUserId(userId);
        comment.setCreateTime(System.currentTimeMillis());
        comment.setUpdateTime(System.currentTimeMillis());
        comment.setCreateUser(userId);
        comment.setUpdateUser(userId);
        commentMapper.insert(comment);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(SuggestionStatusRequest request, String userId) {
        checkAdmin(userId);
        Suggestion suggestion = suggestionMapper.selectByPrimaryKey(request.getId());
        if (suggestion == null) {
            throw new GenericException(Translator.get("suggestion.not_found"));
        }
        extSuggestionMapper.updateStatus(request.getId(), request.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, String userId) {
        Suggestion suggestion = suggestionMapper.selectByPrimaryKey(id);
        if (suggestion == null) {
            throw new GenericException(Translator.get("suggestion.not_found"));
        }
        boolean isAuthor = userId != null && userId.equals(suggestion.getUserId());
        if (!isAdmin(userId) && !isAuthor) {
            throw new GenericException(Translator.get("suggestion.no_permission"));
        }
        LambdaQueryWrapper<SuggestionVote> voteWrapper = new LambdaQueryWrapper<>();
        voteWrapper.eq(SuggestionVote::getSuggestionId, id);
        voteMapper.deleteByLambda(voteWrapper);

        LambdaQueryWrapper<SuggestionComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(SuggestionComment::getSuggestionId, id);
        commentMapper.deleteByLambda(commentWrapper);

        if (StringUtils.isNotBlank(suggestion.getImageIds())) {
            List<String> imageIds = JSON.parseArray(suggestion.getImageIds(), String.class);
            for (String imageId : imageIds) {
                attachmentService.delete(imageId);
                attachmentMapper.deleteByPrimaryKey(imageId);
            }
        }
        suggestionMapper.deleteByPrimaryKey(id);
    }

    private void fillVoted(List<SuggestionDTO> list) {
        String userId = SessionUtils.getUserId();
        if (CollectionUtils.isEmpty(list) || userId == null) {
            return;
        }
        List<String> ids = list.stream().map(Suggestion::getId).toList();
        LambdaQueryWrapper<SuggestionVote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SuggestionVote::getUserId, userId).in(SuggestionVote::getSuggestionId, ids);
        Set<String> votedIds = voteMapper.selectListByLambda(queryWrapper).stream()
                .map(SuggestionVote::getSuggestionId)
                .collect(Collectors.toSet());
        list.forEach(dto -> dto.setVoted(votedIds.contains(dto.getId())));
    }

    private boolean hasVoted(String suggestionId, String userId) {
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<SuggestionVote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SuggestionVote::getSuggestionId, suggestionId).eq(SuggestionVote::getUserId, userId);
        return !CollectionUtils.isEmpty(voteMapper.selectListByLambda(queryWrapper));
    }

    private List<Attachment> loadImages(String imageIdsJson) {
        if (StringUtils.isBlank(imageIdsJson)) {
            return List.of();
        }
        List<String> imageIds = JSON.parseArray(imageIdsJson, String.class);
        if (CollectionUtils.isEmpty(imageIds)) {
            return List.of();
        }
        return attachmentMapper.selectByIds(imageIds);
    }

    private boolean isAdmin(String userId) {
        return InternalUser.ADMIN.getValue().equals(userId);
    }

    private void checkAdmin(String userId) {
        if (!isAdmin(userId)) {
            throw new GenericException(Translator.get("suggestion.admin_only"));
        }
    }
}
