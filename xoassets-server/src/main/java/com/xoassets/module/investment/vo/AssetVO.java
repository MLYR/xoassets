package com.xoassets.module.investment.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 资产返回对象。
 */
@Data
@Builder
public class AssetVO {

    private Long id;
    private String symbol;
    private String name;
    private String type;
    private String currency;
    private String quoteSource;
    private String quoteKey;
}
