package com.xoassets.module.market.scheduler;

import com.xoassets.module.market.service.MarketCalendarService;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 市场日历年度补齐任务，避免新年份没有基础交易日记录。
 */
@Slf4j
@Component
public class MarketCalendarRefreshScheduler {

    private static final String MARKET_A_SHARE = "A_SHARE";

    private final MarketCalendarService marketCalendarService;

    public MarketCalendarRefreshScheduler(MarketCalendarService marketCalendarService) {
        this.marketCalendarService = marketCalendarService;
    }

    /**
     * 每年 1 月 1 日补齐当年和下一年的基础日历，春节等交易所休市日以后续修正数据覆盖。
     */
    @Scheduled(cron = "${xoassets.market-calendar.yearly-refresh-cron:0 5 0 1 1 ?}")
    public void refreshYearlyCalendar() {
        int year = LocalDate.now().getYear();
        try {
            marketCalendarService.ensureYearInitialized(MARKET_A_SHARE, year);
            marketCalendarService.ensureYearInitialized(MARKET_A_SHARE, year + 1);
        } catch (Exception exception) {
            log.warn("市场交易日历年度补齐失败 year={}", year, exception);
        }
    }
}
