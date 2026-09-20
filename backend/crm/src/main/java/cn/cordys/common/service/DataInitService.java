package cn.cordys.common.service;


import cn.cordys.common.util.OnceInterface;
import cn.cordys.common.util.OnceInterfaceAction;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.service.ContractInvoiceService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.order.service.OrderService;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.service.ModuleFieldExtService;
import cn.cordys.crm.system.service.ModuleFieldService;
import cn.cordys.crm.system.service.ModuleFormMigrationService;
import cn.cordys.crm.system.service.ModuleService;
import cn.cordys.crm.system.service.TenantConfigService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Slf4j
public class DataInitService {
    @Resource
    private ModuleService moduleService;
    @Resource
    private ModuleFormMigrationService moduleFormMigrationService;
    @Resource
    private ModuleFieldExtService moduleFieldExtService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;
    @Resource
    private Redisson redisson;
    @Resource
    private ModuleFieldService moduleFieldService;
    @Resource
    private ClueService clueService;
	@Resource
	private ContractService contractService;
	@Resource
	private ContractInvoiceService contractInvoiceService;
	@Resource
	private OpportunityQuotationService opportunityQuotationService;
	@Resource
	private OrderService orderService;
	@Resource
	private TenantConfigService tenantConfigService;

    public void initOneTime() {
        RLock lock = redisson.getLock("init_data_lock");
        lock.lock();
        try {
            initOneTime(moduleService::initDefaultOrgModule, "init.module");
            initOneTime(moduleFormMigrationService::initForm, "init.form");
            initOneTime(moduleFieldService::modifyDateProp, "modify.form.date");
            initOneTime(moduleFormMigrationService::modifyFormLinkProp, "modify.form.link");
            initOneTime(moduleFormMigrationService::modifyFormProp, "modify.form.prop");
            initOneTime(moduleFormMigrationService::modifyFieldMobile, "modify.field.mobile");
            initOneTime(moduleFormMigrationService::processOldLinkData, "process.old.link.data");
            initOneTime(moduleFormMigrationService::initFormScenarioProp, "init.record.form.scenario");
            initOneTime(clueService::processTransferredCluePlanAndRecord, "process.transferred.clue");
            initOneTime(moduleFormMigrationService::initUpgradeForm, "init.upgrade.form.v1.4.0");
            initOneTime(moduleFormMigrationService::initUpgradeForm, "init.upgrade.form.v1.5.0");
            initOneTime(moduleFormMigrationService::initUpgradeForm, "init.upgrade.form.v1.5.1");
            initOneTime(moduleFormMigrationService::initExtFieldsByVer, "1.5.0", "init.ext.fields.v1.5.0");
            initOneTime(moduleFormMigrationService::initExtFieldsByVer, "1.5.1", "init.ext.fields.v1.5.1");
            initOneTime(moduleFieldExtService::setDefaultOptionSource, "set.default.option.source");
            initOneTime(moduleFieldExtService::refreshPlanFieldPos, "refresh.plan.field.pos");
            initOneTime(moduleFormMigrationService::initInvoiceFormScenarioProp, "init.invoice.form.scenario");
            initOneTime(moduleFieldService::modifyInvoiceShowFields, "init.invoice.show.fields");
            initOneTime(moduleService::deleteExtraModules, "delete.extra.modules");
            initOneTime(moduleFieldExtService::modifySubProductSumColumn, "modify.quotation.product.sum.column");
            initOneTime(moduleFormMigrationService::initUpgradeForm, "init.upgrade.form.v1.6.0");
            initOneTime(moduleFieldService::initOrderFields, "init.order.fields");
			initOneTime(moduleFormMigrationService::initContactFormLinkRules, "init.contact.form.link.rules");
			initOneTime(moduleFormMigrationService::initContractToOrderLinkScenario, "init.order.form.link.rules");
            initOneTime(moduleFormMigrationService::initOrderFormScenarioProp, "init.order.form.scenario");
			initOneTime(moduleFieldExtService::modifyInternalSubSumColumn, "modify.internal.sum.column");
			initOneTime(moduleFieldExtService::modifyInternalSubCalcFormula, "modify.internal.calc.formula");
			initOneTime(moduleFieldExtService::refreshFormulaOldReferencedId, "refresh.formula.old.referenced.id");
			initOneTime(contractService::handleOldApprovalData, "handler.contract.approval.status");
			initOneTime(contractInvoiceService::handleOldApprovalData, "handler.contract.invoice.approval.status");
			initOneTime(opportunityQuotationService::handleOldApprovalData, "handler.quotation.approval.status");
			initOneTime(orderService::handleOldApprovalData, "handler.order.approval.status");
			// 回填存量租户缺失的表单配置（须在默认组织表单升级迁移之后执行，才能复制到完整配置）
			initOneTime(moduleFormMigrationService::backfillTenantForms, "backfill.tenant.forms");
			// 回填存量租户缺失的阶段配置（商机/合同/订单），否则新建商机等报 NoSuchElementException
			initOneTime(tenantConfigService::backfillTenantStageConfigs, "backfill.tenant.stage.configs");
			// 回填跟进记录表单的联系人联动（按客户过滤）与跟进时间默认当前日期
			initOneTime(moduleFormMigrationService::backfillRecordFollowConfig, "backfill.record.follow.config");
			// 回填跟进计划表单的联系人联动（按客户过滤）
			initOneTime(moduleFormMigrationService::backfillPlanFollowConfig, "backfill.plan.follow.config");
			// 回填客户表单字段配置（行业/来源改输入框、等级 A~E 级、删客户类型·线上来源）
			initOneTime(moduleFormMigrationService::backfillCustomerFieldConfig, "backfill.customer.field.config");
			// 回填商机表单联系人联动（按客户过滤）
			initOneTime(moduleFormMigrationService::backfillOpportunityContactConfig, "backfill.opportunity.contact.config");
			// 回填线索转客户/转商机的字段联动映射（线索来源等信息带过去）
			initOneTime(moduleFormMigrationService::backfillClueLinkRules, "backfill.clue.link.rules");
			// 回填「来源」类字段全局统一为输入框（线索来源/商机来源），并删除线索线上来源详情
			initOneTime(moduleFormMigrationService::backfillSourceFieldConfig, "backfill.source.field.config");
			// 回填合同回款记录：删除写死的收款银行/账号，新增收款账户数据源与付款凭证附件
			initOneTime(moduleFormMigrationService::backfillContractPaymentRecordFields, "backfill.contract.payment.record.fields");
			// 回填合同表单的金额/累计金额公式（金额=产品单价×数量，累计金额=SUM(合同报价信息.金额)）
			initOneTime(moduleFieldService::initContractFields, "init.contract.fields");
		} finally {
            lock.unlock();
        }
    }

    private void initOneTime(OnceInterface onceFunc, final String key) {
        try {
            LambdaQueryWrapper<Parameter> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Parameter::getParamKey, key);
            List<Parameter> parameters = parameterMapper.selectListByLambda(queryWrapper);
            if (CollectionUtils.isEmpty(parameters)) {
                onceFunc.execute();
                insertParameterOnceKey(key);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 执行单次接口 (带参数)
     *
     * @param onceFunc 执行函数
     * @param param    参数
     * @param key      执行Key
     * @param <P>      参数类型
     */
    private <P> void initOneTime(OnceInterfaceAction<P> onceFunc, P param, final String key) {
        try {
            LambdaQueryWrapper<Parameter> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Parameter::getParamKey, key);
            List<Parameter> parameters = parameterMapper.selectListByLambda(queryWrapper);
            if (CollectionUtils.isEmpty(parameters)) {
                onceFunc.execute(param);
                insertParameterOnceKey(key);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private void insertParameterOnceKey(String key) {
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue("done");
        parameter.setType("text");
        parameterMapper.insert(parameter);
    }
}