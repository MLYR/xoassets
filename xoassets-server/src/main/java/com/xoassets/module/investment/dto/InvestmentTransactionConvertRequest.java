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

    @NotNull(message = "源持仓不能为空")
    private Long sourceHoldingId;

    @NotNull(message = "目标持仓不能为空")
    private Long targetHoldingId;

    @NotNull(message = "资金账户不能为空")
    private Long accountId;

    @NotNull(message = "转出份额不能为空")
    @DecimalMin(value = "0.0000000001", message = "转出份额必须大于0")
    private BigDecimal sourceQuantity;

    @NotNull(message = "转出价格不能为空")
    @DecimalMin(value = "0.0001", message = "转出价格必须大于0")
    private BigDecimal sourcePrice;

    @NotNull(message = "转入份额不能为空")
    @DecimalMin(value = "0.0000000001", message = "转入份额必须大于0")
    private BigDecimal targetQuantity;

    @NotNull(message = "转入价格不能为空")
    @DecimalMin(value = "0.0001", message = "转入价格必须大于0")
    private BigDecimal targetPrice;

    @DecimalMin(value = "0.0000", message = "手续费不能小于0")
    private BigDecimal fee = BigDecimal.ZERO;

    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionTime;

    private String note;
}
