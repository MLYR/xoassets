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

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 来源类型。
     */
    private String sourceType;
    /**
     * 业务类型。
     */
    private String bizType;
    /**
     * 标题。
     */
    private String title;
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
     * 关联账户ID。
     */
    private Long relatedAccountId;
    /**
     * 关联账户名称。
     */
    private String relatedAccountName;
    /**
     * 分类ID。
     */
    private Long categoryId;
    /**
     * 分类名称。
     */
    private String categoryName;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 资产名称。
     */
    private String assetName;
    /**
     * 资产代码。
     */
    private String symbol;
    /**
     * 状态。
     */
    private String status;
    /**
     * 交易发生时间。
     */
    private LocalDateTime transactionTime;
    /**
     * 备注。
     */
    private String note;
}
