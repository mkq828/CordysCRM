package cn.cordys.crm.ai.script.excel;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.ai.script.domain.AiSalesScript;
import cn.cordys.excel.domain.ExcelErrData;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售话术库 Excel 导入监听器：按表头名定位列（固定 4 列：分类/标题/内容/出处），
 * 校验标题/内容必填后组装 {@link AiSalesScript}；错误行以 {@link ExcelErrData} 收集。
 */
@Slf4j
public class AiSalesScriptImportListener extends AnalysisEventListener<Map<Integer, String>> {

    private static final int TITLE_MAX_LENGTH = 128;
    private static final String DEFAULT_SOURCE = "租户自建";

    /** 表头名 -> 列索引 */
    private final Map<String, Integer> headIndexMap = new HashMap<>();

    @Getter
    private final List<AiSalesScript> dataList = new ArrayList<>();
    @Getter
    private final List<ExcelErrData> errList = new ArrayList<>();

    private final String orgId;
    private final String userId;
    private boolean hasData = false;

    public AiSalesScriptImportListener(String orgId, String userId) {
        this.orgId = orgId;
        this.userId = userId;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        headIndexMap.clear();
        if (headMap != null) {
            headMap.forEach((index, name) -> {
                if (StringUtils.isNotBlank(name)) {
                    headIndexMap.put(name.trim(), index);
                }
            });
        }
        if (!headIndexMap.containsKey("标题") || !headIndexMap.containsKey("内容")) {
            throw new GenericException("模板表头不正确，请使用系统下载的导入模板");
        }
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        if (data == null || data.isEmpty()) {
            return;
        }
        hasData = true;
        int rowIndex = context.readRowHolder().getRowIndex();
        String category = cell(data, "分类");
        String title = cell(data, "标题");
        String content = cell(data, "内容");
        String source = cell(data, "出处");

        String error = validate(title, content);
        if (error != null) {
            errList.add(new ExcelErrData(rowIndex, "第 " + (rowIndex + 1) + " 行：" + error));
            return;
        }

        AiSalesScript script = new AiSalesScript();
        script.setId(IDGenerator.nextStr());
        script.setOrganizationId(orgId);
        script.setCategory(category);
        script.setTitle(title);
        script.setContent(content);
        script.setSource(StringUtils.isBlank(source) ? DEFAULT_SOURCE : source);
        script.setCreateUser(userId);
        script.setUpdateUser(userId);
        long now = System.currentTimeMillis();
        script.setCreateTime(now);
        script.setUpdateTime(now);
        dataList.add(script);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!hasData) {
            throw new GenericException("导入文件没有可导入的数据");
        }
    }

    private String cell(Map<Integer, String> data, String header) {
        Integer index = headIndexMap.get(header);
        if (index == null) {
            return null;
        }
        String value = data.get(index);
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private String validate(String title, String content) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isBlank(title)) {
            sb.append("标题不能为空；");
        } else if (title.length() > TITLE_MAX_LENGTH) {
            sb.append("标题不能超过 ").append(TITLE_MAX_LENGTH).append(" 字；");
        }
        if (StringUtils.isBlank(content)) {
            sb.append("内容不能为空；");
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
