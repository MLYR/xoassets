package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 投资资产趋势点位，来自用户投资日快照，供移动端趋势图直接展示真实金额。
 */
@Data
@Builder
public class InvestmentTrendPointVO {

    private LocalDate date;
    private BigDecimal marketValue;
    private BigDecimal totalProfit;
}
