package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 持仓汇总返回对象，复用持仓明细同一套收益口径。
 */
@Data
@Builder
public class HoldingSummaryVO {

    private BigDecimal totalMarketValue;
    private BigDecimal totalCost;
    private BigDecimal todayProfit;
    private BigDecimal yesterdayProfit;
    private BigDecimal floatingProfit;
    private BigDecimal floatingProfitRate;
    private Integer holdingCount;
}
