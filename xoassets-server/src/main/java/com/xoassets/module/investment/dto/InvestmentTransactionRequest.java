package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投资买入和卖出请求参数。
 */
@Data
public class InvestmentTransactionRequest {

    private Long holdingId;

    @NotNull(message = "资产不能为空")
    private Long assetId;

    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    @NotBlank(message = "交易类型不能为空")
    private String type;

    @DecimalMin(value = "0.0000000001", message = "数量必须大于0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0001", message = "价格必须大于0")
    private BigDecimal price;

    private String inputMode = "QUANTITY_PRICE";

    @DecimalMin(value = "0.01", message = "买入总金额必须大于0")
    private BigDecimal tradeAmount;

    private LocalDate confirmedDate;

    @DecimalMin(value = "0.0000", message = "手续费不能小于0")
    private BigDecimal fee = BigDecimal.ZERO;

    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;

    private String note;
}
