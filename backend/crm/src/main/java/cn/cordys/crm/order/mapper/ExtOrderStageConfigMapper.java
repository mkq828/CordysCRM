package cn.cordys.crm.order.mapper;

import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.dto.stage.StageRollBackRequest;
import cn.cordys.common.dto.stage.StageUpdateRequest;
import cn.cordys.crm.order.domain.OrderStageConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtOrderStageConfigMapper {


    List<StageConfigResponse> getStageConfigList(@Param("orgId") String orgId);

    int countStageConfig(@Param("orgId") String orgId);

    void moveUpStageConfig(@Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

    void moveDownStageConfig(@Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

    void updateRollBack(@Param("request") StageRollBackRequest request, @Param("orgId") String orgId);

    void updateStageConfig(@Param("request") StageUpdateRequest request, @Param("userId") String userId, @Param("orgId") String orgId);

    void updatePos(@Param("id") String id, @Param("pos") Long pos, @Param("orgId") String orgId);

    OrderStageConfig getByIdAndOrg(@Param("id") String id, @Param("orgId") String orgId);

    int deleteByIdAndOrg(@Param("id") String id, @Param("orgId") String orgId);
}
