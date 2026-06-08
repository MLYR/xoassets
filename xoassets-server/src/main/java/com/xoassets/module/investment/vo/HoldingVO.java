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

    private Long id;
    private Long assetId;
    private String assetName;
    private String symbol;
    private String assetType;
    private String assetSubType;
    private String profitDisplayMode;
    private String valuationMode;
    private String tradeVenue;
    private String market;
    private String quoteSource;
    private String currency;
    private BigDecimal quantity;
    private BigDecimal avgCost;
    private BigDecimal totalCost;
    private BigDecimal latestPrice;
    private BigDecimal previousPrice;
    private BigDecimal beforePreviousPrice;
    private Integer priceScale;
    private LocalDateTime latestPriceTime;
    private LocalDateTime previousPriceTime;
    private LocalDate priceDate;
    private Boolean todayPriceAvailable;
    private Boolean todayProfitAvailable;
    private String priceStatus;
    private String latestPriceSource;
    private String marketStatus;
    private String primaryProfitLabel;
    private BigDecimal primaryProfitAmount;
    private String secondaryProfitLabel;
    private BigDecimal secondaryProfitAmount;
    private String priceLabel;
    private BigDecimal marketValue;
    private BigDecimal todayProfit;
    private BigDecimal todayProfitBase;
    private BigDecimal todayChangeRate;
    private BigDecimal yesterdayProfit;
    private BigDecimal yesterdayProfitBase;
    private BigDecimal yesterdayChangeRate;
    private BigDecimal floatingProfit;
    private BigDecimal floatingProfitRate;
    private BigDecimal breakEvenRate;
    private String remark;
    private Integer status;
}
