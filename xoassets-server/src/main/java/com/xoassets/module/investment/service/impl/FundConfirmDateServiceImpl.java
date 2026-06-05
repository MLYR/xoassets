package com.xoassets.module.investment.service.impl;

import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.module.investment.service.FundConfirmDateService;
import com.xoassets.module.investment.vo.FundConfirmPreviewVO;
import com.xoassets.module.market.service.MarketCalendarService;
import com.xoassets.persistence.entity.Asset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;

/**
 * 基金确认日期规则：A 股交易日 15:00 前按当天申请，QDII 按 T+2 确认。
 */
@Service
public class FundConfirmDateServiceImpl implements FundConfirmDateService {

    private static final LocalTime FUND_CUTOFF_TIME = LocalTime.of(15, 0);
    private static final String ASSET_TYPE_FUND = "FUND";
    private static final String MARKET_A_SHARE = "A_SHARE";

    private final MarketCalendarService marketCalendarService;

    public FundConfirmDateServiceImpl(MarketCalendarService marketCalendarService) {
        this.marketCalendarService = marketCalendarService;
    }

    @Override
    public LocalDate effectiveTradeDate(Asset asset, LocalDateTime transactionTime) {
        if (asset == null || !ASSET_TYPE_FUND.equals(asset.getType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅基金买入支持确认日期预估");
        }
        if (transactionTime == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易时间不能为空");
        }
        LocalDate tradeDate = transactionTime.toLocalDate();
        boolean afterCutoff = transactionTime.toLocalTime().isAfter(FUND_CUTOFF_TIME);
        LocalDate startDate = afterCutoff ? tradeDate.plusDays(1) : tradeDate;
        return nextTradingDay(startDate);
    }

    @Override
    public LocalDate confirmedDate(Asset asset, LocalDateTime transactionTime) {
        LocalDate effectiveTradeDate = effectiveTradeDate(asset, transactionTime);
        // QDII 净值通常延迟，确认日按 T+2 估算；净值没入库时由待确认扫描继续等待。
        return plusTradingDays(effectiveTradeDate, isQdii(asset) ? 2 : 1);
    }

    @Override
    public FundConfirmPreviewVO preview(Asset asset, LocalDateTime transactionTime) {
        LocalDate effectiveTradeDate = effectiveTradeDate(asset, transactionTime);
        LocalDate tradeDate = transactionTime.toLocalDate();
        boolean afterCutoff = transactionTime.toLocalTime().isAfter(FUND_CUTOFF_TIME);
        boolean shifted = !effectiveTradeDate.equals(tradeDate);
        boolean qdii = isQdii(asset);
        return FundConfirmPreviewVO.builder()
                .tradeDate(tradeDate)
                .effectiveTradeDate(effectiveTradeDate)
                .confirmedDate(plusTradingDays(effectiveTradeDate, qdii ? 2 : 1))
                .qdii(qdii)
                .shifted(shifted)
                .shiftReason(shiftReason(tradeDate, afterCutoff, shifted))
                .build();
    }

    @Override
    public boolean isTradingDay(LocalDate date) {
        if (date == null) {
            return false;
        }
        return marketCalendarService.isTradingDay(MARKET_A_SHARE, date);
    }

    private LocalDate nextTradingDay(LocalDate date) {
        LocalDate cursor = date;
        while (!isTradingDay(cursor)) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    private LocalDate plusTradingDays(LocalDate startDate, int days) {
        LocalDate cursor = startDate;
        int remaining = days;
        while (remaining > 0) {
            cursor = cursor.plusDays(1);
            if (isTradingDay(cursor)) {
                remaining--;
            }
        }
        return cursor;
    }

    private boolean isQdii(Asset asset) {
        String name = asset.getName() == null ? "" : asset.getName().toUpperCase();
        return name.contains("QDII");
    }

    private String shiftReason(LocalDate tradeDate, boolean afterCutoff, boolean shifted) {
        if (!shifted) {
            return null;
        }
        if (afterCutoff) {
            return "实际买入时间已超过15:00";
        }
        if (!isTradingDay(tradeDate)) {
            return "实际买入日期为非交易日";
        }
        return "实际买入日期已顺延";
    }
}
