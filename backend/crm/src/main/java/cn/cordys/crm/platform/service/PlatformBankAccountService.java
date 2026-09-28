package cn.cordys.crm.platform.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.platform.constants.PlatformPaymentType;
import cn.cordys.crm.platform.domain.PlatformBankAccount;
import cn.cordys.crm.platform.dto.request.PlatformBankAccountSaveRequest;
import cn.cordys.crm.platform.dto.response.PlatformBankAccountResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 平台收款账号服务：我方公司各收款方式的收款账户（每种方式一条），新增回款时自动带出
 */
@Service
public class PlatformBankAccountService {

    @Resource
    private BaseMapper<PlatformBankAccount> bankAccountMapper;

    /**
     * 列表：按 4 种收款方式补齐（未配置的返回空账号，方便前端固定行展示）
     */
    public List<PlatformBankAccountResponse> list() {
        Map<String, PlatformBankAccount> byType = bankAccountMapper
                .selectListByLambda(new LambdaQueryWrapper<PlatformBankAccount>())
                .stream()
                .collect(Collectors.toMap(PlatformBankAccount::getAccountType, Function.identity(), (a, b) -> a));
        return Arrays.stream(PlatformPaymentType.values())
                .map(type -> toResponse(byType.get(type.name()), type.name()))
                .toList();
    }

    /**
     * 批量保存：按收款方式 upsert（每种方式至多一条）
     */
    public void save(List<PlatformBankAccountSaveRequest> requests, String operatorId) {
        long now = System.currentTimeMillis();
        for (PlatformBankAccountSaveRequest request : requests) {
            if (request == null || StringUtils.isBlank(request.getAccountType())) {
                continue;
            }
            PlatformBankAccount account = bankAccountMapper
                    .selectListByLambda(new LambdaQueryWrapper<PlatformBankAccount>()
                            .eq(PlatformBankAccount::getAccountType, request.getAccountType()))
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (account == null) {
                account = new PlatformBankAccount();
                account.setId(IDGenerator.nextStr());
                account.setAccountType(request.getAccountType());
                account.setCreateTime(now);
                account.setCreateUser(operatorId);
                account.setUpdateTime(now);
                account.setUpdateUser(operatorId);
                account.setAccountName(request.getAccountName());
                account.setAccountNo(request.getAccountNo());
                account.setBankName(request.getBankName());
                bankAccountMapper.insert(account);
            } else {
                account.setAccountName(request.getAccountName());
                account.setAccountNo(request.getAccountNo());
                account.setBankName(request.getBankName());
                account.setUpdateTime(now);
                account.setUpdateUser(operatorId);
                bankAccountMapper.updateById(account);
            }
        }
    }

    private PlatformBankAccountResponse toResponse(PlatformBankAccount account, String type) {
        PlatformBankAccountResponse response = new PlatformBankAccountResponse();
        response.setAccountType(type);
        if (account != null) {
            response.setId(account.getId());
            response.setAccountName(account.getAccountName());
            response.setAccountNo(account.getAccountNo());
            response.setBankName(account.getBankName());
        }
        return response;
    }
}
