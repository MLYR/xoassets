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

    @NotBlank(message = "流水类型不能为空")
    private String type;

    @NotNull(message = "金额必填")
    @DecimalMin(value = "0.0001", message = "金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "账户不能为空")
    private Long accountId;

    private Long targetAccountId;
    private Long categoryId;
    private Long originalTransactionId;

    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;

    private String note;
    private String imageUrl;
}
