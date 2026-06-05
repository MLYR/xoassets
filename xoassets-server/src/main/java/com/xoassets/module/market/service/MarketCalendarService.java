package com.xoassets.module.market.service;

import java.time.LocalDate;

/**
 * 市场交易日历服务，屏蔽数据库补齐和交易日判断细节。
 */
public interface MarketCalendarService {

    /**
     * 判断指定市场日期是否为交易日。
     */
    boolean isTradingDay(String market, LocalDate date);

    /**
     * 补齐指定市场和年份的基础交易日历。
     */
    void ensureYearInitialized(String market, int year);
}
