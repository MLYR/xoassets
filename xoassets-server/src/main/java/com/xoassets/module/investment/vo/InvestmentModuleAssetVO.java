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

    private String module;
    private String name;
    private BigDecimal assetAmount;
    private BigDecimal assetRatio;
    private String primaryProfitLabel;
    private Boolean primaryProfitAvailable;
    private BigDecimal primaryProfitAmount;
    private String primaryProfitStatusLabel;
    private BigDecimal holdingProfit;
    private BigDecimal holdingProfitRate;
    private Integer holdingCount;
}
