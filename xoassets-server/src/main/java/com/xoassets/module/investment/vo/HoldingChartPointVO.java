package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 持仓详情图表点位，后端基于真实价格快照和当前持仓口径计算金额曲线。
 */
@Data
@Builder
public class HoldingChartPointVO {

    private LocalDateTime quoteTime;
    private BigDecimal totalAssetAmount;
    private BigDecimal totalProfitAmount;
}
