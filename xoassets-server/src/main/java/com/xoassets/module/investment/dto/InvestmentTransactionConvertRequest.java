package com.xoassets.module.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 投资持仓转换请求参数：在同一资金账户内卖出源持仓并买入目标持仓。
 */
@Data
public class InvestmentTransactionConvertRequest {

    /**
     * 转出持仓ID。
     */
    @NotNull(message = "源持仓不能为空")
    private Long sourceHoldingId;

    /**
     * 转入持仓ID。
     */
    @NotNull(message = "目标持仓不能为空")
    private Long targetHoldingId;

    /**
     * 账户ID。
     */
    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    /**
     * 转出数量。
     */
    @NotNull(message = "转出份额不能为空")
    @DecimalMin(value = "0.0000000001", message = "转出份额必须大于0")
    private BigDecimal sourceQuantity;

    /**
     * 转出价格。
     */
    @NotNull(message = "转出价格不能为空")
    @DecimalMin(value = "0.0001", message = "转出价格必须大于0")
    private BigDecimal sourcePrice;

    /**
     * 转入数量。
     */
    @NotNull(message = "转入份额不能为空")
    @DecimalMin(value = "0.0000000001", message = "转入份额必须大于0")
    private BigDecimal targetQuantity;

    /**
     * 转入价格。
     */
    @NotNull(message = "转入价格不能为空")
    @DecimalMin(value = "0.0001", message = "转入价格必须大于0")
    private BigDecimal targetPrice;

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
