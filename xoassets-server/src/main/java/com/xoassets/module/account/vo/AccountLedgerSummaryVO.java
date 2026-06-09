package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 账户资金明细汇总，撤销投资交易不参与合计。
 */
@Data
@Builder
public class AccountLedgerSummaryVO {

    /**
     * 当前余额。
     */
    private BigDecimal currentBalance;
    /**
     * 初始余额。
     */
    private BigDecimal initialBalance;
    /**
     * 累计流入。
     */
    private BigDecimal totalInflow;
    /**
     * 累计流出。
     */
    private BigDecimal totalOutflow;
    /**
     * 净流入金额。
     */
    private BigDecimal netInflow;
    /**
     * 流水数量。
     */
    private long transactionCount;
}
