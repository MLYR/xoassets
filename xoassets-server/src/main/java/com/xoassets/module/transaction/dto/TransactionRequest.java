package com.xoassets.module.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 流水新增和修改请求参数。
 */
@Data
public class TransactionRequest {

    /**
     * 业务类型。
     */
    @NotBlank(message = "流水类型不能为空")
    private String type;

    /**
     * 金额。
     */
    @NotNull(message = "金额必填")
    @DecimalMin(value = "0.0001", message = "金额必须大于0")
    private BigDecimal amount;

    /**
     * 账户ID。
     */
    @NotNull(message = "账户不能为空")
    private Long accountId;

    /**
     * 转入账户ID。
     */
    private Long targetAccountId;
    /**
     * 分类ID。
     */
    private Long categoryId;
    /**
     * 原流水ID。
     */
    private Long originalTransactionId;

    /**
     * 交易发生时间。
     */
    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;

    /**
     * 备注。
     */
    private String note;
    /**
     * 图片地址。
     */
    private String imageUrl;
}
