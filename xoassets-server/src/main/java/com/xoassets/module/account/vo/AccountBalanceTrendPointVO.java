package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 账户日终余额曲线点。
 */
@Data
@Builder
public class AccountBalanceTrendPointVO {

    /**
     * 日期。
     */
    private String date;
    /**
     * 日终余额。
     */
    private BigDecimal endBalance;
    /**
     * 流入金额。
     */
    private BigDecimal inflow;
    /**
     * 流出金额。
     */
    private BigDecimal outflow;
    /**
     * 余额修正金额。
     */
    private BigDecimal adjustmentAmount;
}
