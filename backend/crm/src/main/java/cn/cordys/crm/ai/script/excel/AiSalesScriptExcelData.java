package cn.cordys.crm.ai.script.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 销售话术库导入模板列定义（用于生成下载模板的表头：分类/标题/内容/出处）。
 */
@Data
@ColumnWidth(20)
public class AiSalesScriptExcelData {

    @ExcelProperty("分类")
    private String category;

    @ExcelProperty("标题")
    @ColumnWidth(30)
    private String title;

    @ExcelProperty("内容")
    @ColumnWidth(60)
    private String content;

    @ExcelProperty("出处")
    @ColumnWidth(30)
    private String source;
}
