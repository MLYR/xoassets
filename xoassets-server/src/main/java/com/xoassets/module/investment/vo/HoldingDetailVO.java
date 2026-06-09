package com.xoassets.module.investment.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 持仓详情返回对象，聚合持仓估值、交易汇总、交易记录和价格快照。
 */
@Data
@Builder
public class HoldingDetailVO {

    /**
     * 持仓信息。
     */
    private HoldingVO holding;
    /**
     * 摘要。
     */
    private HoldingDetailSummaryVO summary;
    /**
     * 交易记录。
     */
    private List<InvestmentTransactionVO> transactions;
    /**
     * 价格快照列表。
     */
    private List<AssetPriceVO> priceSnapshots;
    /**
     * 图表点位。
     */
    private List<HoldingChartPointVO> chartPoints;
    /**
     * 收益日历。
     */
    private List<InvestmentCalendarDayProfitVO> profitCalendar;
}
