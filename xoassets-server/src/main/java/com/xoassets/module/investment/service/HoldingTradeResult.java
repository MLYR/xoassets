package com.xoassets.module.investment.service;

import com.xoassets.persistence.entity.Holding;
import java.math.BigDecimal;

/**
 * 持仓买卖联动结果，供投资交易记录写入成本和已实现盈亏。
 */
public record HoldingTradeResult(Holding holding, BigDecimal sellCost, BigDecimal realizedProfit) {
}
