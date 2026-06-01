package com.xoassets.module.exchange.service;

import com.xoassets.module.exchange.vo.ExchangeRateVO;

/**
 * 汇率服务，第一版用于投资页 CNY / USD 展示换算。
 */
public interface ExchangeRateService {

    /**
     * 获取 USD/CNY 汇率，优先返回当天缓存。
     */
    ExchangeRateVO usdCny();

    /**
     * 刷新 USD/CNY 汇率缓存。
     */
    ExchangeRateVO refreshUsdCny();
}
