package com.xoassets.module.market.scheduler;

import com.xoassets.module.market.service.MarketCalendarService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * 市场日历年度补齐任务，避免新年份没有基础交易日记录。
 */
@Slf4j
@Component
public class MarketCalendarRefreshScheduler {

    /**
     * A股市场常量。
     */
    private static final String MARKET_A_SHARE = "A_SHARE";

    /**
     * 交易日历服务。
     */
    private final MarketCalendarService marketCalendarService;

    /**
     * 注入定时任务依赖。
     */
    public MarketCalendarRefreshScheduler(MarketCalendarService marketCalendarService) {
        this.marketCalendarService = marketCalendarService;
    }

    /**
     * 应用启动时先补当前年和下一年基础日历，避免全新库等到下一次年度定时任务前没有交易日数据。
     */
    @PostConstruct
    public void initializeCurrentCalendars() {
        refreshCalendar(LocalDate.now().getYear());
    }

    /**
     * 每年 1 月 1 日补齐当年和下一年的基础日历，春节等交易所休市日以后续修正数据覆盖。
     */
    @XxlJob("refreshYearlyMarketCalendar")
    public void refreshYearlyCalendar() {
        refreshCalendar(LocalDate.now().getYear());
    }

    /**
     * 刷新交易日历。
     */
    private void refreshCalendar(int year) {
        try {
            marketCalendarService.ensureYearInitialized(MARKET_A_SHARE, year);
            marketCalendarService.ensureYearInitialized(MARKET_A_SHARE, year + 1);
        } catch (Exception exception) {
            log.warn("市场交易日历年度补齐失败 year={}", year, exception);
        }
    }
}
