package com.xoassets.module.exchange.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.xoassets.module.exchange.service.ExchangeRateService;
import com.xoassets.module.exchange.vo.ExchangeRateVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 汇率服务实现；MVP 先使用进程内日缓存，后续接 Redis 时只替换缓存读写，不影响前端接口。
 */
@Slf4j
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    /**
     * 默认美元兑人民币汇率。
     */
    private static final BigDecimal DEFAULT_USD_CNY = new BigDecimal("7.2000");

    /**
     * HTTP客户端。
     */
    private final RestClient restClient = RestClient.builder().baseUrl("https://open.er-api.com").build();
    /**
     * 美元兑人民币汇率进程内缓存。
     */
    private volatile ExchangeRateVO usdCnyCache = ExchangeRateVO.builder()
            .baseCurrency("USD")
            .targetCurrency("CNY")
            .rate(DEFAULT_USD_CNY)
            .source("DEFAULT")
            .quoteTime(LocalDateTime.now())
            .build();

    /**
     * 当天缓存直接复用，减少第三方汇率接口请求。
     */
    @Override
    public ExchangeRateVO usdCny() {
        if (usdCnyCache.getQuoteTime() != null && usdCnyCache.getQuoteTime().toLocalDate().isEqual(LocalDateTime.now().toLocalDate())) {
            return usdCnyCache;
        }
        return refreshUsdCny();
    }

    /**
     * 刷新 USD/CNY 汇率；失败时保留旧缓存，页面展示不受第三方波动影响。
     */
    @Override
    public ExchangeRateVO refreshUsdCny() {
        try {
            JsonNode response = restClient.get()
                    .uri("/v6/latest/USD")
                    .retrieve()
                    .body(JsonNode.class);
            BigDecimal rate = new BigDecimal(response.path("rates").path("CNY").asText())
                    .setScale(4, RoundingMode.HALF_UP);
            usdCnyCache = ExchangeRateVO.builder()
                    .baseCurrency("USD")
                    .targetCurrency("CNY")
                    .rate(rate)
                    .source("ER_API")
                    .quoteTime(LocalDateTime.now())
                    .build();
        } catch (Exception exception) {
            log.warn("USD/CNY 汇率刷新失败，继续使用缓存 rate={}", usdCnyCache.getRate(), exception);
        }
        return usdCnyCache;
    }

    /**
     * 每天早上刷新一次汇率缓存；失败只记录日志，不影响主应用。
     */
    @XxlJob("refreshDailyUsdCnyExchangeRate")
    public void refreshDaily() {
        refreshUsdCny();
    }
}
