package cn.cordys.crm.system.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.BugReport;
import cn.cordys.crm.system.dto.request.BugReportListRequest;
import cn.cordys.crm.system.dto.request.BugReportRequest;
import cn.cordys.crm.system.dto.response.BugReportDetailResponse;
import cn.cordys.crm.system.dto.response.BugReportResponse;
import cn.cordys.crm.system.mapper.ExtBugReportMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 问题反馈服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BugReportService {

    private static final String STATUS_PENDING = "PENDING";

    @Resource
    private BaseMapper<BugReport> bugReportMapper;

    @Resource
    private ExtBugReportMapper extBugReportMapper;

    /**
     * 提交问题反馈（普通用户）
     */
    public void submit(BugReportRequest request) {
        BugReport bugReport = BeanUtils.copyBean(new BugReport(), request);
        bugReport.setId(IDGenerator.nextStr());
        bugReport.setOrganizationId(OrganizationContext.getOrganizationId());
        bugReport.setUserId(SessionUtils.getUserId());
        bugReport.setUserName(SessionUtils.getUser() == null ? null : SessionUtils.getUser().getName());
        bugReport.setStatus(STATUS_PENDING);
        bugReport.setCreateTime(System.currentTimeMillis());
        bugReportMapper.insert(bugReport);
    }

    /**
     * 问题反馈列表查询（admin）
     * <p>admin 平台超管跨租户查看全部问题反馈，普通用户仅看本企业。</p>
     */
    public List<BugReportResponse> list(BugReportListRequest request, String orgId) {
        String filterOrgId = isAdmin() ? null : orgId;
        return extBugReportMapper.list(request, filterOrgId);
    }

    /**
     * 问题反馈详情（admin）
     * <p>admin 平台超管可查看任意租户的问题反馈。</p>
     */
    public BugReportDetailResponse getDetail(String id, String orgId) {
        BugReport bugReport = bugReportMapper.selectByPrimaryKey(id);
        if (bugReport == null || (!isAdmin() && !Objects.equals(bugReport.getOrganizationId(), orgId))) {
            throw new GenericException(Translator.get("bug_report.not_found"));
        }
        return BeanUtils.copyBean(new BugReportDetailResponse(), bugReport);
    }

    /**
     * 是否平台超管（硬编码 admin 账号）。
     */
    private boolean isAdmin() {
        return InternalUser.ADMIN.getValue().equals(SessionUtils.getUserId());
    }
}
