package com.xoassets.module.investment.provider;

import com.xoassets.persistence.entity.Asset;

/**
 * 行情刷新抽象，后续股票、基金和更多数据源都实现该接口。
 */
public interface QuoteProvider {

    /**
     * 判断当前 provider 是否能处理该资产。
     */
    boolean supports(Asset asset);

    /**
     * 拉取最新行情；失败时抛出业务异常，由调用方保留旧价格。
     */
    QuoteFetchResult fetch(Asset asset);
}
