package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 投资交易返回对象。
 */
@Data
@Builder
public class InvestmentTransactionVO {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 持仓ID。
     */
    private Long holdingId;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 账户ID。
     */
    private Long accountId;
    /**
     * 账户名称。
     */
    private String accountName;
    /**
     * 资产名称。
     */
    private String assetName;
    /**
     * 资产代码。
     */
    private String symbol;
    /**
     * 业务类型。
     */
    private String type;
    /**
     * 交易录入模式。
     */
    private String inputMode;
    /**
     * 交易金额。
     */
    private BigDecimal tradeAmount;
    /**
     * 成交数量。
     */
    private BigDecimal tradeQuantity;
    /**
     * 成交价格。
     */
    private BigDecimal tradePrice;
    /**
     * 持仓数量。
     */
    private BigDecimal quantity;
    /**
     * 价格。
     */
    private BigDecimal price;
    /**
     * 金额。
     */
    private BigDecimal amount;
    /**
     * 手续费。
     */
    private BigDecimal fee;
    /**
     * 成本金额。
     */
    private BigDecimal costAmount;
    /**
     * 已实现收益。
     */
    private BigDecimal realizedProfit;
    /**
     * 交易日期。
     */
    private LocalDate tradeDate;
    /**
     * 确认日期。
     */
    private LocalDate confirmedDate;
    /**
     * 确认净值。
     */
    private BigDecimal confirmedNav;
    /**
     * 确认份额。
     */
    private BigDecimal confirmedQuantity;
    /**
     * 状态。
     */
    private String status;
    /**
     * 撤销时间。
     */
    private java.time.LocalDateTime revokeTime;
    /**
     * 撤销原因。
     */
    private String revokeReason;
    /**
     * 交易发生时间。
     */
    private LocalDateTime transactionTime;
    /**
     * 备注。
     */
    private String note;
}
