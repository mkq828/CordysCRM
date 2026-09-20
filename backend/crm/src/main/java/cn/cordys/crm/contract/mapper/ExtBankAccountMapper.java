package cn.cordys.crm.contract.mapper;

import cn.cordys.crm.contract.dto.request.BankAccountPageRequest;
import cn.cordys.crm.contract.dto.response.BankAccountListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtBankAccountMapper {


    List<BankAccountListResponse> list(@Param("request") BankAccountPageRequest request, @Param("orgId") String orgId, @Param("userId") String userId);

    int countByName(@Param("name") String name, @Param("orgId") String orgId, @Param("id") String id);
}
