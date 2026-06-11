package com.xoassets.module.analytics.vo;

import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import com.xoassets.module.investment.vo.InvestmentModuleAssetVO;
import com.xoassets.module.investment.vo.InvestmentTrendVO;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 数据分析页投资聚合对象。
 */
@Data
@Builder
public class InvestmentAnalyticsVO {

    /**
     * 投资资产总额。
     */
    private BigDecimal totalInvestmentAsset;
    /**
     * 持有收益。
     */
    private BigDecimal holdingProfit;
    /**
     * 持有收益率。
     */
    private BigDecimal holdingProfitRate;
    /**
     * 今日收益。
     */
    private BigDecimal todayProfit;
    /**
     * 今日收益是否可用。
     */
    private Boolean todayProfitAvailable;
    /**
     * 今日收益状态文案。
     */
    private String todayProfitStatusLabel;
    /**
     * 昨日收益。
     */
    private BigDecimal yesterdayProfit;
    /**
     * 模块资产列表。
     */
    private List<InvestmentModuleAssetVO> moduleAssets;
    /**
     * 投资趋势。
     */
    private InvestmentTrendVO trend;
    /**
     * 每日收益日历。
     */
    private List<InvestmentCalendarDayProfitVO> dailyProfitCalendar;
    /**
     * 当前模块持仓列表。
     */
    private List<HoldingVO> holdings;
}
