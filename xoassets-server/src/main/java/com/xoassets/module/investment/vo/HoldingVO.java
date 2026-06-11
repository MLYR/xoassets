package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 持仓返回对象，包含最新价格、市值和浮动盈亏。
 */
@Data
@Builder
public class HoldingVO {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 资产名称。
     */
    private String assetName;
    /**
     * 资产代码。
     */
    private String symbol;
    /**
     * 资产类型。
     */
    private String assetType;
    /**
     * 资产子类型。
     */
    private String assetSubType;
    /**
     * 收益展示模式。
     */
    private String profitDisplayMode;
    /**
     * 估值模式。
     */
    private String valuationMode;
    /**
     * 交易场所。
     */
    private String tradeVenue;
    /**
     * 交易市场。
     */
    private String market;
    /**
     * 行情来源。
     */
    private String quoteSource;
    /**
     * 币种。
     */
    private String currency;
    /**
     * 持仓数量。
     */
    private BigDecimal quantity;
    /**
     * 平均成本。
     */
    private BigDecimal avgCost;
    /**
     * 总成本。
     */
    private BigDecimal totalCost;
    /**
     * 最新价格。
     */
    private BigDecimal latestPrice;
    /**
     * 上一交易日价格。
     */
    private BigDecimal previousPrice;
    /**
     * 上上交易日价格。
     */
    private BigDecimal beforePreviousPrice;
    /**
     * 价格展示小数位。
     */
    private Integer priceScale;
    /**
     * 最新价格时间。
     */
    private LocalDateTime latestPriceTime;
    /**
     * 上一交易日价格时间。
     */
    private LocalDateTime previousPriceTime;
    /**
     * 价格日期。
     */
    private LocalDate priceDate;
    /**
     * 今日价格是否可用。
     */
    private Boolean todayPriceAvailable;
    /**
     * 今日收益是否可用。
     */
    private Boolean todayProfitAvailable;
    /**
     * 价格状态。
     */
    private String priceStatus;
    /**
     * 最新价格来源。
     */
    private String latestPriceSource;
    /**
     * 市场状态。
     */
    private String marketStatus;
    /**
     * 主收益指标名称。
     */
    private String primaryProfitLabel;
    /**
     * 主收益金额。
     */
    private BigDecimal primaryProfitAmount;
    /**
     * 副收益指标名称。
     */
    private String secondaryProfitLabel;
    /**
     * 副收益金额。
     */
    private BigDecimal secondaryProfitAmount;
    /**
     * 价格展示文案。
     */
    private String priceLabel;
    /**
     * 持仓市值。
     */
    private BigDecimal marketValue;
    /**
     * 今日收益。
     */
    private BigDecimal todayProfit;
    /**
     * 今日收益计算基准。
     */
    private BigDecimal todayProfitBase;
    /**
     * 今日收益率。
     */
    private BigDecimal todayProfitRate;
    /**
     * 今日涨跌幅。
     */
    private BigDecimal todayChangeRate;
    /**
     * 按当前份额计算的今日收益。
     */
    private BigDecimal todayProfitByCurrentQuantity;
    /**
     * 按当前份额计算的今日收益率。
     */
    private BigDecimal todayProfitRateByCurrentQuantity;
    /**
     * 按上一交易日日终份额计算的今日收益。
     */
    private BigDecimal todayProfitByPreviousSnapshotQuantity;
    /**
     * 按上一交易日日终份额计算的今日收益率。
     */
    private BigDecimal todayProfitRateByPreviousSnapshotQuantity;
    /**
     * 昨日收益。
     */
    private BigDecimal yesterdayProfit;
    /**
     * 昨日收益计算基准。
     */
    private BigDecimal yesterdayProfitBase;
    /**
     * 昨日涨跌幅。
     */
    private BigDecimal yesterdayChangeRate;
    /**
     * 浮动盈亏。
     */
    private BigDecimal floatingProfit;
    /**
     * 已实现收益。
     */
    private BigDecimal realizedProfit;
    /**
     * 总收益，已实现收益 + 当前浮动盈亏。
     */
    private BigDecimal totalProfit;
    /**
     * 总收益率。
     */
    private BigDecimal totalProfitRate;
    /**
     * 浮动盈亏率。
     */
    private BigDecimal floatingProfitRate;
    /**
     * 回本所需涨跌幅。
     */
    private BigDecimal breakEvenRate;
    /**
     * 备注。
     */
    private String remark;
    /**
     * 状态。
     */
    private Integer status;
}
