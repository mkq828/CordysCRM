package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.request.BugReportListRequest;
import cn.cordys.crm.system.dto.response.BugReportResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtBugReportMapper {

    List<BugReportResponse> list(@Param("request") BugReportListRequest request, @Param("orgId") String orgId);
}
