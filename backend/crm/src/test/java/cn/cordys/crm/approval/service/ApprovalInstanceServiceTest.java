package cn.cordys.crm.approval.service;

import cn.cordys.common.domain.BaseModel;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalFlowVersion;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 审批实例服务测试
 *
 * <p>覆盖 {@link ApprovalInstanceService#clearApprovingInstanceOfFlow(String)}：
 * 删除审批流时需跨多张表（实例、任务、记录等）清除审批中的数据，该操作必须保证事务一致性。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApprovalInstanceServiceTest extends BaseTest {

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Resource
    private BaseMapper<ApprovalFlowVersion> approvalFlowVersionMapper;

    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;

    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;

    @Resource
    private BaseMapper<ApprovalRecord> approvalRecordMapper;

    @Test
    void testClearApprovingInstanceOfFlow() {
        String flowId = "test_flow_" + System.currentTimeMillis();
        String versionId = "test_version_" + System.currentTimeMillis();

        // 1. 创建审批流版本
        ApprovalFlowVersion version = new ApprovalFlowVersion();
        version.setId(versionId);
        version.setFlowId(flowId);
        version.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        version.setCreateUser("admin");
        version.setCreateTime(System.currentTimeMillis());
        approvalFlowVersionMapper.insert(version);

        // 2. 创建审批中的实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId("test_instance_" + System.currentTimeMillis());
        instance.setFlowVersionId(versionId);
        instance.setType(ApprovalFormTypeEnum.QUOTATION.getValue());
        instance.setResourceId("test_resource_" + System.currentTimeMillis());
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        setAuditFields(instance);
        approvalInstanceMapper.insert(instance);

        // 3. 创建审批任务
        ApprovalTask task = new ApprovalTask();
        task.setId("test_task_" + System.currentTimeMillis());
        task.setInstanceId(instance.getId());
        task.setNodeId("test_node");
        task.setApproverId("admin");
        setAuditFields(task);
        approvalTaskMapper.insert(task);

        // 4. 创建审批记录
        ApprovalRecord record = new ApprovalRecord();
        record.setId("test_record_" + System.currentTimeMillis());
        record.setInstanceId(instance.getId());
        record.setTaskId(task.getId());
        setAuditFields(record);
        approvalRecordMapper.insert(record);

        // 5. 清除该审批流下的审批中数据
        approvalInstanceService.clearApprovingInstanceOfFlow(flowId);

        // 6. 断言实例、任务、记录均被物理删除
        Assertions.assertNull(approvalInstanceMapper.selectByPrimaryKey(instance.getId()));
        Assertions.assertNull(approvalTaskMapper.selectByPrimaryKey(task.getId()));
        Assertions.assertNull(approvalRecordMapper.selectByPrimaryKey(record.getId()));

        // 7. 审批流版本本身不应被删除
        Assertions.assertNotNull(approvalFlowVersionMapper.selectByPrimaryKey(versionId));
    }

    private void setAuditFields(BaseModel model) {
        long now = System.currentTimeMillis();
        model.setCreateUser("admin");
        model.setUpdateUser("admin");
        model.setCreateTime(now);
        model.setUpdateTime(now);
    }
}
