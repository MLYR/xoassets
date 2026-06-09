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

    /**
     * 持仓ID。
     */
    private Long holdingId;

    /**
     * 资产ID。
     */
    @NotNull(message = "资产不能为空")
    private Long assetId;

    /**
     * 账户ID。
     */
    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    /**
     * 业务类型。
     */
    @NotBlank(message = "交易类型不能为空")
    private String type;

    /**
     * 持仓数量。
     */
    @DecimalMin(value = "0.0000000001", message = "数量必须大于0")
    private BigDecimal quantity;

    /**
     * 价格。
     */
    @DecimalMin(value = "0.0001", message = "价格必须大于0")
    private BigDecimal price;

    /**
     * 交易录入模式。
     */
    private String inputMode = "QUANTITY_PRICE";

    /**
     * 交易金额。
     */
    @DecimalMin(value = "0.01", message = "买入总金额必须大于0")
    private BigDecimal tradeAmount;

    /**
     * 确认日期。
     */
    private LocalDate confirmedDate;

    /**
     * 手续费。
     */
    @DecimalMin(value = "0.0000", message = "手续费不能小于0")
    private BigDecimal fee = BigDecimal.ZERO;

    /**
     * 交易发生时间。
     */
    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;

    /**
     * 备注。
     */
    private String note;
}
