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

    /**
     * 月份。
     */
    private YearMonth month;
    /**
     * 收入金额。
     */
    private BigDecimal income;
    /**
     * 支出金额。
     */
    private BigDecimal expense;
    /**
     * 账户余额。
     */
    private BigDecimal balance;
}
