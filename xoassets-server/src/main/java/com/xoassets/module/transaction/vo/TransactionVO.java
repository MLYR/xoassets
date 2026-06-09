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

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 业务类型。
     */
    private String type;
    /**
     * 金额。
     */
    private BigDecimal amount;
    /**
     * 账户ID。
     */
    private Long accountId;
    /**
     * 账户名称。
     */
    private String accountName;
    /**
     * 转入账户ID。
     */
    private Long targetAccountId;
    /**
     * 转入账户名称。
     */
    private String targetAccountName;
    /**
     * 分类ID。
     */
    private Long categoryId;
    /**
     * 分类名称。
     */
    private String categoryName;
    /**
     * 原流水ID。
     */
    private Long originalTransactionId;
    /**
     * 交易发生时间。
     */
    private LocalDateTime transactionTime;
    /**
     * 备注。
     */
    private String note;
    /**
     * 图片地址。
     */
    private String imageUrl;
    /**
     * 状态。
     */
    private Integer status;
}
