package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.request.TenantPlanPageRequest;
import cn.cordys.crm.system.dto.response.TenantPlanResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户套餐扩展 Mapper
 */
public interface ExtTenantPlanMapper {

    /**
     * 分页查询租户套餐列表（含组织名称/类型、管理员手机号、最后登录时间）
     */
    List<TenantPlanResponse> pageList(@Param("request") TenantPlanPageRequest request);
}
