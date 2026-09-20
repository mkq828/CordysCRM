package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtModuleFieldMapper {

    /**
     * 根据ID集合批量删除字段
     *
     * @param ids ID集合
     */
    void deleteByIds(@Param("ids") List<String> ids);

    /**
     * 根据ID集合批量删除字段属性
     *
     * @param ids ID集合
     */
    void deletePropByIds(@Param("ids") List<String> ids);

    List<OptionDTO> getSourceOptionsByIds(@Param("tableName") String table, @Param("ids") List<String> ids);
    List<OptionDTO> getCustomFormOptionsByIds(@Param("ids") List<String> ids);
	List<OptionDTO> getSourceOptionsByName(@Param("tableName") String table, @Param("keyword") String keyword, @Param("orgId") String orgId);
	List<OptionDTO> getCustomFormOptionsByName(@Param("keyword") String keyword, @Param("orgId") String orgId);

	List<ModuleField> getModuleField(@Param("orgId") String orgId, @Param("formKeys") List<String> formKeys);

    /**
     * 批量更新移动端显示
     *
     * @param ids    ID集合
     * @param mobile 移动端显示
     */
    void batchUpdateMobile(@Param("ids") List<String> ids, @Param("mobile") Boolean mobile);

	/**
	 * 获取表单字段最大位次
	 * @param formId 表单ID
	 * @return 位置下标
	 */
	Long getMaxFieldPosByFormId(String formId);

	/**
	 * 获取表单子表格字段集合
	 * @param formKey 表单Key
	 * @param orgId   组织ID
	 * @return 子表格字段集合
	 */
	List<ModuleFieldBlob> getFormSubFields(@Param("formKey") String formKey, @Param("orgId") String orgId);

	/**
	 * 字段选项码转文本（一次性数据迁移：SELECT 改 INPUT 后回填旧值）
	 *
	 * @param tableName 字段值表名（如 clue_field / opportunity_field）
	 * @param fieldId   字段ID
	 * @param oldValue  旧选项码
	 * @param newValue  新文本
	 */
	void updateFieldValueCodeToText(@Param("tableName") String tableName, @Param("fieldId") String fieldId,
									@Param("oldValue") String oldValue, @Param("newValue") String newValue);
}
