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

    private HoldingVO holding;
    private HoldingDetailSummaryVO summary;
    private List<InvestmentTransactionVO> transactions;
    private List<AssetPriceVO> priceSnapshots;
    private List<HoldingChartPointVO> chartPoints;
    private List<InvestmentCalendarDayProfitVO> profitCalendar;
}
