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

    private String date;
    private BigDecimal endBalance;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal adjustmentAmount;
}
