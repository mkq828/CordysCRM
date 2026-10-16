package cn.cordys.crm.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 经营大屏「主动预警」异常项。一条异常 = 一类预警规则命中一次，点卡片可联动下钻到对应维度。
 */
@Data
@Schema(description = "主动预警异常项")
public class AlertItem {

    public static final String TYPE_CONTRACT_DROP = "CONTRACT_DROP";
    public static final String TYPE_STALE_BIG_CUSTOMER = "STALE_BIG_CUSTOMER";
    public static final String TYPE_STALE_OWNER = "STALE_OWNER";

    @Schema(description = "预警类型: CONTRACT_DROP/STALE_BIG_CUSTOMER/STALE_OWNER")
    private String type;

    @Schema(description = "级别: WARNING/CRITICAL")
    private String level;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String desc;

    @Schema(description = "下钻目标维度: trend/sales/customer")
    private String targetKind;

    @Schema(description = "下钻目标键(销售维度为 ownerId，其余为空)")
    private String targetKey;

    @Schema(description = "实体ID(客户预警为 customerId)")
    private String entityId;

    @Schema(description = "实体名称(客户名/负责人名)")
    private String entityName;

    @Schema(description = "最近跟进时间(毫秒，久未联系类预警用)")
    private Long lastFollowTime;

    @Schema(description = "环比预警：本月金额")
    private BigDecimal currentAmount;

    @Schema(description = "环比预警：上月金额")
    private BigDecimal previousAmount;
}
