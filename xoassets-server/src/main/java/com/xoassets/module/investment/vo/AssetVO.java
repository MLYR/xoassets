package com.xoassets.module.investment.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 资产返回对象。
 */
@Data
@Builder
public class AssetVO {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 资产代码。
     */
    private String symbol;
    /**
     * 名称。
     */
    private String name;
    /**
     * 业务类型。
     */
    private String type;
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
}
