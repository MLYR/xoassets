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
 * 用户投资资产日快照实体，用于较昨日、较上月和 AI 投资复盘。
 */
@Data
@TableName("xo_investment_daily_snapshot")
public class InvestmentDailySnapshot {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 快照日期。
     */
    private LocalDate snapshotDate;
    /**
     * 持仓市值。
     */
    private BigDecimal marketValue;
    /**
     * 总成本。
     */
    private BigDecimal totalCost;
    /**
     * 浮动盈亏。
     */
    private BigDecimal floatingProfit;
    /**
     * 浮动盈亏率。
     */
    private BigDecimal floatingProfitRate;
    /**
     * 已实现收益。
     */
    private BigDecimal realizedProfit;
    /**
     * 快照日资金流调整收益：本日投资市值 - 上一快照日投资市值 - 当日投资本金净流入。
     */
    private BigDecimal dailyProfit;
    /**
     * 快照日资金流调整收益率。
     */
    private BigDecimal dailyProfitRate;
    /**
     * 当日投资本金净流入，买入为正，卖出为负。
     */
    private BigDecimal netInflow;
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
