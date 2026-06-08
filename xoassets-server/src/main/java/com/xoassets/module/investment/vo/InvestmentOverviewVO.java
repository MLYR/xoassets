package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 投资总览返回对象，今日收益按今日有效价格动态汇总。
 */
@Data
@Builder
public class InvestmentOverviewVO {

    private BigDecimal totalInvestmentAsset;
    private BigDecimal totalCost;
    private BigDecimal holdingProfit;
    private BigDecimal holdingProfitRate;
    private Boolean todayProfitAvailable;
    private BigDecimal todayProfit;
    private String todayProfitAssetScope;
    private String todayProfitStatusLabel;
    private BigDecimal yesterdayProfit;
    private String yesterdayProfitAssetScope;
    private List<InvestmentModuleAssetVO> moduleAssets;
}
