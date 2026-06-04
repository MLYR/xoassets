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

    private Long id;
    private Long userId;
    private LocalDate snapshotDate;
    private BigDecimal marketValue;
    private BigDecimal totalCost;
    private BigDecimal floatingProfit;
    private BigDecimal floatingProfitRate;
    private BigDecimal realizedProfit;
    private BigDecimal dailyProfit;
    private BigDecimal dailyProfitRate;
    private BigDecimal netInflow;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
