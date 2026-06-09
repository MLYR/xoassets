package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 投资资产趋势点位，来自用户投资日快照，供移动端趋势图直接展示真实金额。
 */
@Data
@Builder
public class InvestmentTrendPointVO {

    /**
     * 日期。
     */
    private LocalDate date;
    /**
     * 持仓市值。
     */
    private BigDecimal marketValue;
    /**
     * 总收益。
     */
    private BigDecimal totalProfit;
    /**
     * 资产金额。
     */
    private BigDecimal assetAmount;
    /**
     * 持有收益。
     */
    private BigDecimal holdingProfit;
    /**
     * 当日收益，ALL 趋势按所有持仓收益日历同日汇总。
     */
    private BigDecimal dailyProfit;
    /**
     * 当日收益率。
     */
    private BigDecimal dailyProfitRate;
    /**
     * 主收益指标名称。
     */
    private String primaryProfitLabel;
    /**
     * 主收益金额。
     */
    private BigDecimal primaryProfitAmount;
}
