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

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
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
    private LocalDateTime revokeTime;
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
    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;
}
