package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 投资总览返回对象，把实时资产今日收益和净值型资产昨日收益拆开，避免前端混用口径。
 */
@Data
@Builder
public class InvestmentOverviewVO {

    private BigDecimal totalInvestmentAsset;
    private BigDecimal totalCost;
    private BigDecimal holdingProfit;
    private BigDecimal holdingProfitRate;
    private BigDecimal todayProfit;
    private String todayProfitAssetScope;
    private BigDecimal yesterdayProfit;
    private String yesterdayProfitAssetScope;
    private List<InvestmentModuleAssetVO> moduleAssets;
}
