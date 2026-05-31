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

    private String name;
    private String type;
    private BigDecimal value;
    private BigDecimal percent;
}
