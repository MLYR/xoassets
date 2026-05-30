package com.xoassets.module.statistics.vo;

import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.Builder;
import lombok.Data;

/**
 * 月度收支趋势点。
 */
@Data
@Builder
public class IncomeExpenseTrendVO {

    private YearMonth month;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
}
