package cn.cordys.crm.contract.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.domain.BankAccount;
import cn.cordys.crm.contract.dto.request.BankAccountAddRequest;
import cn.cordys.crm.contract.dto.request.BankAccountPageRequest;
import cn.cordys.crm.contract.dto.request.BankAccountUpdateRequest;
import cn.cordys.crm.contract.dto.response.BankAccountListResponse;
import cn.cordys.crm.contract.mapper.ExtBankAccountMapper;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class BankAccountService {

    @Resource
    private BaseMapper<BankAccount> bankAccountMapper;
    @Resource
    private ExtBankAccountMapper extBankAccountMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private AttachmentService attachmentService;


    /**
     * 添加收款账户
     *
     * @param request 请求
     * @param userId  用户ID
     * @param orgId   组织ID
     * @return 收款账户
     */
    public BankAccount add(BankAccountAddRequest request, String userId, String orgId) {
        checkName(request.getName(), orgId, null);

        BankAccount bankAccount = BeanUtils.copyBean(new BankAccount(), request);
        bankAccount.setCreateTime(System.currentTimeMillis());
        bankAccount.setCreateUser(userId);
        bankAccount.setUpdateTime(System.currentTimeMillis());
        bankAccount.setUpdateUser(userId);
        bankAccount.setId(IDGenerator.nextStr());
        bankAccount.setOrganizationId(orgId);

        bankAccountMapper.insert(bankAccount);
        processQrcode(bankAccount.getId(), bankAccount.getQrcode(), orgId, userId);
        return bankAccount;
    }


    /**
     * 校验账户名称
     *
     * @param name  账户名称
     * @param orgId 组织ID
     * @param id    id
     */
    private void checkName(String name, String orgId, String id) {
        if (extBankAccountMapper.countByName(name, orgId, id) > 0) {
            throw new GenericException(Translator.get("bank_account.exist"));
        }
    }


    /**
     * 编辑收款账户
     *
     * @param request 请求
     * @param userId  用户ID
     * @param orgId   组织ID
     * @return 收款账户
     */
    public BankAccount update(BankAccountUpdateRequest request, String userId, String orgId) {
        checkAccount(request.getId());
        checkName(request.getName(), orgId, request.getId());

        BankAccount bankAccount = BeanUtils.copyBean(new BankAccount(), request);
        bankAccount.setUpdateTime(System.currentTimeMillis());
        bankAccount.setUpdateUser(userId);
        bankAccountMapper.update(bankAccount);
        processQrcode(bankAccount.getId(), bankAccount.getQrcode(), orgId, userId);
        return bankAccount;
    }

    private BankAccount checkAccount(String id) {
        BankAccount bankAccount = bankAccountMapper.selectByPrimaryKey(id);
        if (bankAccount == null) {
            throw new GenericException(Translator.get("bank_account.not.exist"));
        }
        return bankAccount;
    }

    /**
     * 转存收款二维码临时附件为正式附件（绑定到收款账户）
     *
     * @param accountId 收款账户ID
     * @param qrcode    二维码附件ID
     * @param orgId     组织ID
     * @param userId    用户ID
     */
    private void processQrcode(String accountId, String qrcode, String orgId, String userId) {
        List<String> tempFileIds = StringUtils.isBlank(qrcode) ? Collections.emptyList() : List.of(qrcode);
        attachmentService.processTemp(new UploadTransferRequest(orgId, accountId, userId, tempFileIds));
    }


    /**
     * 删除
     *
     * @param id id
     */
    public void delete(String id) {
        BankAccount bankAccount = checkAccount(id);
        bankAccountMapper.deleteByPrimaryKey(id);
        if (StringUtils.isNotBlank(bankAccount.getQrcode())) {
            attachmentService.delete(bankAccount.getQrcode());
        }
    }


    /**
     * 列表
     *
     * @param request 请求
     * @param userId  用户ID
     * @param orgId   组织ID
     * @return 分页列表
     */
    public Pager<List<BankAccountListResponse>> list(BankAccountPageRequest request, String userId, String orgId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<BankAccountListResponse> list = extBankAccountMapper.list(request, orgId, userId);
        baseService.setCreateAndUpdateUserName(list);
        return PageUtils.setPageInfo(page, list);
    }


    /**
     * 详情
     *
     * @param id id
     * @return 收款账户详情
     */
    public BankAccountListResponse get(String id) {
        BankAccount bankAccount = checkAccount(id);
        BankAccountListResponse bankAccountListResponse = BeanUtils.copyBean(new BankAccountListResponse(), bankAccount);
        baseService.setCreateAndUpdateUserName(List.of(bankAccountListResponse));
        return bankAccountListResponse;
    }

    /**
     * 获取收款账户详情 （⚠️反射调用; 勿修改入参, 返回, 方法名!）
     *
     * @param id 收款账户ID
     * @return 收款账户详情
     */
    public BankAccountListResponse getSimple(String id) {
        BankAccount bankAccount = bankAccountMapper.selectByPrimaryKey(id);
        if (bankAccount == null) {
            return null;
        }
        return BeanUtils.copyBean(new BankAccountListResponse(), bankAccount);
    }

    /**
     * 批量获取收款账户详情 (用于数据源批量查询优化)
     *
     * @param ids 收款账户ID集合
     * @return 收款账户详情列表
     */
    public List<BankAccountListResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<BankAccount> accounts = bankAccountMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(accounts)) {
            return Collections.emptyList();
        }
        return accounts.stream().map(account -> BeanUtils.copyBean(new BankAccountListResponse(), account)).toList();
    }

    public ModuleFormConfigDTO getBusinessFormConfig() {
        ModuleFormConfigDTO moduleFormConfigDTO = new ModuleFormConfigDTO();
        List<BaseField> fields = moduleFormService.initBankAccountFields();
        moduleFormConfigDTO.setFields(fields);
        return moduleFormConfigDTO;
    }

    public List<BankAccount> selectByIds(List<String> ids) {
        return bankAccountMapper.selectByIds(ids);
    }

    public String getBankAccountName(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        BankAccount bankAccount = bankAccountMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(bankAccount).map(BankAccount::getName).orElse(null);
    }

    /**
     * 通过名称获取收款账户集合
     *
     * @param names 名称
     * @return 收款账户集合
     */
    public List<BankAccount> getBankAccountListByNames(List<String> names) {
        LambdaQueryWrapper<BankAccount> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(BankAccount::getName, names);
        return bankAccountMapper.selectListByLambda(lambdaQueryWrapper);
    }

    /**
     * 通过ID集合获取收款账户名称
     *
     * @param ids id集合
     * @return 收款账户名称
     */
    public String getBankAccountNameByIds(List<String> ids) {
        List<BankAccount> accounts = bankAccountMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(accounts)) {
            List<String> names = accounts.stream().map(BankAccount::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }
}
