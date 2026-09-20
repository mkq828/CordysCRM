package cn.cordys.crm.suggestion.mapper;

import cn.cordys.crm.suggestion.dto.request.SuggestionPageRequest;
import cn.cordys.crm.suggestion.dto.response.SuggestionCommentDTO;
import cn.cordys.crm.suggestion.dto.response.SuggestionDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtSuggestionMapper {

    List<SuggestionDTO> page(@Param("request") SuggestionPageRequest request);

    SuggestionDTO detail(@Param("id") String id);

    List<SuggestionCommentDTO> listComments(@Param("suggestionId") String suggestionId);

    int updateVoteCount(@Param("id") String id, @Param("delta") int delta);

    int updateStatus(@Param("id") String id, @Param("status") String status);
}
