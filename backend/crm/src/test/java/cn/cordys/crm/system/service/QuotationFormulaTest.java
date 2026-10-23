package cn.cordys.crm.system.service;

import cn.cordys.common.formula.FormulaDefinitionParser;
import cn.cordys.common.formula.FormulaEngine;
import cn.cordys.common.formula.FormulaEvaluationContext;
import cn.cordys.common.formula.FormulaFieldMetadata;
import cn.cordys.common.formula.FormulaNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证报价金额公式：金额 = 产品定价 × 折扣 × (1 + 税点)，税点按 percent 自动 ÷100。
 */
class QuotationFormulaTest {

    private final FormulaEngine engine = new FormulaEngine();
    private final FormulaDefinitionParser parser = new FormulaDefinitionParser();

    @Test
    void quotationAmountFormulaEvaluatesPriceTimesDiscountTimesOnePlusTax() throws Exception {
        String formula = invokeFormula("getQuotationProductAmountFormula", "p", "d", "t");

        FormulaNode node = parser.parse(formula);
        assertFalse(node instanceof FormulaNode.Invalid, "公式 JSON 应能解析：" + formula);

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("p", 100);
        values.put("d", 0.9);
        values.put("t", 6);
        Map<String, FormulaFieldMetadata> metadata = Map.of(
                "p", new FormulaFieldMetadata("产品定价", "INPUT_NUMBER",
                        FormulaFieldMetadata.ValueType.NUMBER, FormulaFieldMetadata.NumberType.NUMBER, false),
                "d", new FormulaFieldMetadata("折扣", "INPUT_NUMBER",
                        FormulaFieldMetadata.ValueType.NUMBER, FormulaFieldMetadata.NumberType.NUMBER, false),
                "t", new FormulaFieldMetadata("税点", "INPUT_NUMBER",
                        FormulaFieldMetadata.ValueType.NUMBER, FormulaFieldMetadata.NumberType.PERCENT, false)
        );

        Object result = engine.evaluate(formula,
                new FormulaEvaluationContext(values, metadata, LocalDateTime.now()));

        // 100 × 0.9 × (1 + 6/100) = 95.4
        assertEquals(95.4D, result);
    }

    @Test
    void quotationTotalAmountFormulaIsValidSum() throws Exception {
        String formula = invokeFormula("getQuotationTotalAmountFormula", "products.amount");

        FormulaNode node = parser.parse(formula);
        assertFalse(node instanceof FormulaNode.Invalid, "累计金额公式应能解析：" + formula);
    }

    private String invokeFormula(String methodName, String... args) throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        java.util.Arrays.fill(paramTypes, String.class);
        Method method = ModuleFieldService.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return (String) method.invoke(new ModuleFieldService(), (Object[]) args);
    }
}
