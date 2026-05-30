package com.xoassets.module.transaction.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 流水返回对象，补充账户名和分类名，方便前端表格直接展示。
 */
@Data
@Builder
public class TransactionVO {

    private Long id;
    private String type;
    private BigDecimal amount;
    private Long accountId;
    private String accountName;
    private Long targetAccountId;
    private String targetAccountName;
    private Long categoryId;
    private String categoryName;
    private Long originalTransactionId;
    private LocalDateTime transactionTime;
    private String note;
    private Integer status;
}
