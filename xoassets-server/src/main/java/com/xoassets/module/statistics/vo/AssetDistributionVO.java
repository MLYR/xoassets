package com.xoassets.module.statistics.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 资产分布图表项。
 */
@Data
@Builder
public class AssetDistributionVO {

    /**
     * 名称。
     */
    private String name;
    /**
     * 业务类型。
     */
    private String type;
    /**
     * 下钻引用ID。
     */
    private Long refId;
    /**
     * 下钻引用类型：ACCOUNT / HOLDING。
     */
    private String refType;
    /**
     * 数值。
     */
    private BigDecimal value;
    /**
     * 占比。
     */
    private BigDecimal percent;
}
