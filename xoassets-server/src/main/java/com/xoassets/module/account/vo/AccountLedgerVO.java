package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 账户资金明细统一展示对象，聚合普通流水和投资交易。
 */
@Data
@Builder
public class AccountLedgerVO {

    private Long id;
    private String sourceType;
    private String bizType;
    private String title;
    private BigDecimal amount;
    private Long accountId;
    private String accountName;
    private Long relatedAccountId;
    private String relatedAccountName;
    private Long categoryId;
    private String categoryName;
    private Long assetId;
    private String assetName;
    private String symbol;
    private String status;
    private LocalDateTime transactionTime;
    private String note;
}
