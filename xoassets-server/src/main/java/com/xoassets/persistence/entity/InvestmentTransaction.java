package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投资交易流水实体，记录买入和卖出操作。
 */
@Data
@TableName("xo_investment_transaction")
public class InvestmentTransaction {

    private Long id;
    private Long userId;
    private Long holdingId;
    private Long assetId;
    private Long accountId;
    private String type;
    private String inputMode;
    private BigDecimal tradeAmount;
    private BigDecimal tradeQuantity;
    private BigDecimal tradePrice;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal costAmount;
    private BigDecimal realizedProfit;
    private LocalDate tradeDate;
    private LocalDate confirmedDate;
    private BigDecimal confirmedNav;
    private BigDecimal confirmedQuantity;
    private String status;
    private LocalDateTime revokeTime;
    private String revokeReason;
    private LocalDateTime transactionTime;
    private String note;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
