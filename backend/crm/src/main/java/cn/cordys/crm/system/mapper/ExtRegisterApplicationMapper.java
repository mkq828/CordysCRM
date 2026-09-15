package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.RegisterApplication;
import cn.cordys.crm.system.dto.request.RegisterApplicationPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 注册申请单扩展 Mapper
 */
public interface ExtRegisterApplicationMapper {

    /**
     * 统计 sys_user 中身份证号哈希出现次数（个人实名去重）
     */
    int countByIdCardHashInUser(@Param("idCardHash") String idCardHash);

    /**
     * 统计申请单中身份证号哈希出现次数（待审/驳回去重）
     */
    int countByIdCardHashInApplication(@Param("idCardHash") String idCardHash);

    /**
     * 统计 sys_organization 中统一社会信用代码出现次数（企业去重）
     */
    int countByCreditCodeInOrganization(@Param("creditCode") String creditCode);

    /**
     * 统计申请单中统一社会信用代码出现次数（企业去重）
     */
    int countByCreditCodeInApplication(@Param("creditCode") String creditCode);

    /**
     * 统计申请单中手机号出现次数
     */
    int countByPhoneInApplication(@Param("phone") String phone);

    /**
     * 分页查询申请单列表
     */
    List<RegisterApplication> pageList(@Param("request") RegisterApplicationPageRequest request);
}
