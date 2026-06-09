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

    /**
     * 投资资产总额。
     */
    private BigDecimal totalInvestmentAsset;
    /**
     * 总成本。
     */
    private BigDecimal totalCost;
    /**
     * 持有收益。
     */
    private BigDecimal holdingProfit;
    /**
     * 持有收益率。
     */
    private BigDecimal holdingProfitRate;
    /**
     * 今日收益是否可用。
     */
    private Boolean todayProfitAvailable;
    /**
     * 今日收益。
     */
    private BigDecimal todayProfit;
    /**
     * 今日收益资产范围。
     */
    private String todayProfitAssetScope;
    /**
     * 今日收益状态文案。
     */
    private String todayProfitStatusLabel;
    /**
     * 昨日收益。
     */
    private BigDecimal yesterdayProfit;
    /**
     * 昨日收益资产范围。
     */
    private String yesterdayProfitAssetScope;
    /**
     * 模块资产列表。
     */
    private List<InvestmentModuleAssetVO> moduleAssets;
}
