package com.xoassets.module.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 资产趋势点。
 */
@Data
@Builder
public class AssetTrendPointVO {

    private LocalDate date;
    private BigDecimal value;
}
