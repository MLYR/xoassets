package com.xoassets.module.investment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xoassets.module.investment.vo.FundConfirmPreviewVO;
import com.xoassets.module.market.service.MarketCalendarService;
import com.xoassets.persistence.entity.Asset;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 基金申购申请日和确认日规则测试。
 */
class FundConfirmDateServiceImplTest {

    private final FundConfirmDateServiceImpl service = new FundConfirmDateServiceImpl(testCalendarService());

    @Test
    void beforeCutoffUsesSameTradingDayAndQdiiT2() {
        FundConfirmPreviewVO preview = service.preview(fund("长城全球新能源车股票(QDII)C"), LocalDateTime.of(2026, 6, 4, 14, 59));

        assertEquals(LocalDate.of(2026, 6, 4), preview.getEffectiveTradeDate());
        assertEquals(LocalDate.of(2026, 6, 8), preview.getConfirmedDate());
        assertTrue(preview.getQdii());
        assertFalse(preview.getShifted());
    }

    @Test
    void afterCutoffShiftsToNextTradingDay() {
        FundConfirmPreviewVO preview = service.preview(fund("长城全球新能源车股票(QDII)C"), LocalDateTime.of(2026, 6, 4, 15, 1));

        assertEquals(LocalDate.of(2026, 6, 5), preview.getEffectiveTradeDate());
        assertEquals(LocalDate.of(2026, 6, 9), preview.getConfirmedDate());
        assertTrue(preview.getShifted());
        assertEquals("实际买入时间已超过15:00", preview.getShiftReason());
    }

    @Test
    void weekendShiftsToNextTradingDay() {
        FundConfirmPreviewVO preview = service.preview(fund("普通基金"), LocalDateTime.of(2026, 6, 6, 10, 0));

        assertEquals(LocalDate.of(2026, 6, 8), preview.getEffectiveTradeDate());
        assertEquals(LocalDate.of(2026, 6, 9), preview.getConfirmedDate());
        assertTrue(preview.getShifted());
    }

    @Test
    void holidayShiftsToNextTradingDay() {
        FundConfirmPreviewVO preview = service.preview(fund("普通基金"), LocalDateTime.of(2026, 6, 19, 10, 0));

        assertEquals(LocalDate.of(2026, 6, 22), preview.getEffectiveTradeDate());
        assertEquals(LocalDate.of(2026, 6, 23), preview.getConfirmedDate());
        assertTrue(preview.getShifted());
    }

    @Test
    void regularFundUsesT1() {
        FundConfirmPreviewVO preview = service.preview(fund("普通基金"), LocalDateTime.of(2026, 6, 4, 9, 30));

        assertEquals(LocalDate.of(2026, 6, 5), preview.getConfirmedDate());
        assertFalse(preview.getQdii());
    }

    private Asset fund(String name) {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setType("FUND");
        asset.setName(name);
        return asset;
    }

    private MarketCalendarService testCalendarService() {
        // 单元测试用内存日历模拟数据库结果，业务服务本身不再持有年度休市硬编码。
        Set<LocalDate> holidays = Set.of(
                LocalDate.of(2026, 6, 19),
                LocalDate.of(2026, 6, 20),
                LocalDate.of(2026, 6, 21));
        return new MarketCalendarService() {
            @Override
            public boolean isTradingDay(String market, LocalDate date) {
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY && !holidays.contains(date);
            }

            @Override
            public void ensureYearInitialized(String market, int year) {
                // 测试不需要落库补齐。
            }
        };
    }
}
