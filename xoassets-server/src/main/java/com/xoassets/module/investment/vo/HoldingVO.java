package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
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
    private String currency;
    private BigDecimal quantity;
    private BigDecimal avgCost;
    private BigDecimal totalCost;
    private BigDecimal latestPrice;
    private LocalDateTime latestPriceTime;
    private BigDecimal marketValue;
    private BigDecimal floatingProfit;
    private BigDecimal floatingProfitRate;
    private String remark;
    private Integer status;
}
