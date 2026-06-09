package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 投资模块资产卡片返回对象，用于总览页区分基金、股票和虚拟货币各自收益口径。
 */
@Data
@Builder
public class InvestmentModuleAssetVO {

    /**
     * 投资模块。
     */
    private String module;
    /**
     * 名称。
     */
    private String name;
    /**
     * 资产金额。
     */
    private BigDecimal assetAmount;
    /**
     * 资产占比。
     */
    private BigDecimal assetRatio;
    /**
     * 主收益指标名称。
     */
    private String primaryProfitLabel;
    /**
     * 主收益是否可用。
     */
    private Boolean primaryProfitAvailable;
    /**
     * 主收益金额。
     */
    private BigDecimal primaryProfitAmount;
    /**
     * 主收益状态文案。
     */
    private String primaryProfitStatusLabel;
    /**
     * 持有收益。
     */
    private BigDecimal holdingProfit;
    /**
     * 持有收益率。
     */
    private BigDecimal holdingProfitRate;
    /**
     * 持仓数量。
     */
    private Integer holdingCount;
}
