package com.xoassets.module.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xoassets.module.market.service.MarketCalendarService;
import com.xoassets.persistence.entity.MarketCalendar;
import com.xoassets.persistence.mapper.MarketCalendarMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 市场交易日历数据库实现；没有年度数据时先补基础周末规则，再读取数据库结果。
 */
@Service
public class MarketCalendarServiceImpl implements MarketCalendarService {

    /**
     * 系统工作日来源常量。
     */
    private static final String SOURCE_SYSTEM_WEEKDAY = "SYSTEM_WEEKDAY";
    /**
     * 交易日历优先级排序SQL。
     */
    private static final String CALENDAR_PRIORITY_SQL = "order by case source when 'MANUAL' then 3 when 'EXCHANGE_ANNOUNCEMENT' then 2 when 'SYSTEM_WEEKDAY' then 1 else 0 end desc, id desc limit 1";

    /**
     * 交易日历数据访问组件。
     */
    private final MarketCalendarMapper marketCalendarMapper;

    /**
     * 注入业务依赖。
     */
    public MarketCalendarServiceImpl(MarketCalendarMapper marketCalendarMapper) {
        this.marketCalendarMapper = marketCalendarMapper;
    }

    /**
     * 判断是否交易日。
     */
    @Override
    public boolean isTradingDay(String market, LocalDate date) {
        if (market == null || market.isBlank() || date == null) {
            return false;
        }
        MarketCalendar calendar = findCalendar(market, date);
        if (calendar == null) {
            ensureYearInitialized(market, date.getYear());
            calendar = findCalendar(market, date);
        }
        // 数据库补齐失败时退回周末规则，避免确认日计算陷入死循环。
        return calendar == null ? isWeekday(date) : Boolean.TRUE.equals(calendar.getTradingDay());
    }

    /**
     * 确保年份交易日历已初始化。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void ensureYearInitialized(String market, int year) {
        if (market == null || market.isBlank()) {
            return;
        }
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<MarketCalendar> existingRows = marketCalendarMapper.selectList(new LambdaQueryWrapper<MarketCalendar>()
                .eq(MarketCalendar::getMarket, market)
                .between(MarketCalendar::getTradeDate, start, end));
        Set<LocalDate> existingDates = new HashSet<>(existingRows.stream().map(MarketCalendar::getTradeDate).toList());
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            if (!existingDates.contains(cursor)) {
                insertBaselineDay(market, cursor);
            }
            cursor = cursor.plusDays(1);
        }
    }

    /**
     * 查询交易日历配置。
     */
    private MarketCalendar findCalendar(String market, LocalDate date) {
        return marketCalendarMapper.selectOne(new LambdaQueryWrapper<MarketCalendar>()
                .eq(MarketCalendar::getMarket, market)
                .eq(MarketCalendar::getTradeDate, date)
                // 同一天可能既有系统周末规则又有交易所公告修正，确认日和休市展示必须优先取修正记录。
                .last(CALENDAR_PRIORITY_SQL));
    }

    /**
     * 写入基础交易日。
     */
    private void insertBaselineDay(String market, LocalDate date) {
        MarketCalendar calendar = new MarketCalendar();
        calendar.setId(IdWorker.getId());
        calendar.setMarket(market);
        calendar.setTradeDate(date);
        calendar.setTradingDay(isWeekday(date));
        calendar.setSource(SOURCE_SYSTEM_WEEKDAY);
        calendar.setRemark("系统按周末规则自动补齐，交易所休市日以数据库修正记录为准");
        calendar.setDeleted(0);
        marketCalendarMapper.insert(calendar);
    }

    /**
     * 判断业务条件是否成立。
     */
    private boolean isWeekday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}
