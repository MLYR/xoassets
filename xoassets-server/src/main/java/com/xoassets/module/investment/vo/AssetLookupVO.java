package com.xoassets.module.investment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 资产自动识别结果，供前端新增持仓时选择并自动填充资产信息。
 */
@Data
@Builder
public class AssetLookupVO {

    /**
     * 名称。
     */
    private String name;
    /**
     * 资产代码。
     */
    private String symbol;
    /**
     * 资产类型。
     */
    private String assetType;
    /**
     * 交易市场。
     */
    private String market;
    /**
     * 币种。
     */
    private String currency;
    /**
     * 行情来源。
     */
    private String quoteSource;
    /**
     * 行情查询键。
     */
    private String quoteKey;
    /**
     * 最新价格。
     */
    private BigDecimal latestPrice;
    /**
     * 上一交易日收盘价。
     */
    private BigDecimal previousClose;
    /**
     * 较上一交易日涨跌幅。
     */
    private BigDecimal changePercent;
    /**
     * 报价时间。
     */
    private LocalDateTime quoteTime;
}
