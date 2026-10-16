package cn.cordys.crm.dashboard.dto.response;

import lombok.Data;

/**
 * 某人多日无跟进预警的原始行（负责人姓名由服务层富化）。lastFollowTime=0 表示从未跟进。
 */
@Data
public class StaleOwnerRow {

    private String owner;

    private Long lastFollowTime;
}
