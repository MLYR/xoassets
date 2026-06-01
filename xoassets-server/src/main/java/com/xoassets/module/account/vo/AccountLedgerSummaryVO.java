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

    private BigDecimal currentBalance;
    private BigDecimal initialBalance;
    private BigDecimal totalInflow;
    private BigDecimal totalOutflow;
    private BigDecimal netInflow;
    private long transactionCount;
}
