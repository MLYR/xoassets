package com.xoassets.module.investment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.persistence.entity.Asset;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * CoinGecko 行情 provider，阶段二只支持常见虚拟货币现价。
 */
@Component
public class CoinGeckoQuoteProvider implements QuoteProvider {

    /**
     * 虚拟货币资产类型常量。
     */
    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";
    /**
     * CoinGecko行情来源常量。
     */
    private static final String SOURCE_COINGECKO = "COINGECKO";
    /**
     * 支持的虚拟货币代码映射。
     */
    private static final Map<String, String> SUPPORTED_COINS = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "SOL", "solana",
            "BNB", "binancecoin",
            "DOGE", "dogecoin");

    /**
     * HTTP客户端。
     */
    private final RestClient restClient;
    /**
     * JSON序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 初始化行情提供方。
     */
    public CoinGeckoQuoteProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 使用 CoinGecko keyless public API，后续如需 Pro API 再抽配置。
        this.restClient = RestClient.builder().baseUrl("https://api.coingecko.com/api/v3").build();
    }

    /**
     * 判断是否支持该资产。
     */
    @Override
    public boolean supports(Asset asset) {
        return ASSET_TYPE_CRYPTO.equals(asset.getType()) && SOURCE_COINGECKO.equals(asset.getQuoteSource());
    }

    /**
     * 拉取行情数据。
     */
    @Override
    public QuoteFetchResult fetch(Asset asset) {
        String coinId = resolveCoinId(asset);
        String vsCurrency = StringUtils.hasText(asset.getCurrency()) ? asset.getCurrency().toLowerCase(Locale.ROOT) : "cny";
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/simple/price")
                            .queryParam("ids", coinId)
                            .queryParam("vs_currencies", vsCurrency)
                            .queryParam("include_24hr_change", "true")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode priceNode = response == null ? null : response.path(coinId).path(vsCurrency);
            if (priceNode == null || !priceNode.isNumber()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "CoinGecko 未返回有效价格");
            }
            BigDecimal price = priceNode.decimalValue().setScale(8, RoundingMode.HALF_UP);
            BigDecimal changePercent = changePercent(response, coinId, vsCurrency);
            BigDecimal previousClose = previousClose(price, changePercent);
            BigDecimal changeAmount = previousClose == null ? null : price.subtract(previousClose).setScale(8, RoundingMode.HALF_UP);
            return new QuoteFetchResult(
                    // 行情价格保留 8 位，避免 DOGE 等低单价币种展示价和市值计算口径不一致。
                    price,
                    vsCurrency.toUpperCase(Locale.ROOT),
                    previousClose,
                    changeAmount,
                    changePercent,
                    SOURCE_COINGECKO,
                    LocalDateTime.now(),
                    "OPEN",
                    objectMapper.writeValueAsString(response));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "CoinGecko 行情刷新失败");
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "CoinGecko 行情解析失败");
        }
    }

    /**
     * quote_key 优先支持 CoinGecko id，也支持 BTC/ETH/SOL/BNB/DOGE 简写。
     */
    private String resolveCoinId(Asset asset) {
        String key = StringUtils.hasText(asset.getQuoteKey()) ? asset.getQuoteKey() : asset.getSymbol();
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "CoinGecko 行情键不能为空");
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        String coinId = SUPPORTED_COINS.getOrDefault(normalized, key.trim().toLowerCase(Locale.ROOT));
        if (!SUPPORTED_COINS.containsValue(coinId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "CoinGecko 第一版只支持 BTC、ETH、SOL、BNB、DOGE");
        }
        return coinId;
    }

    /**
     * CoinGecko 返回 24h 涨跌幅时，推导上一价格用于今日收益展示；缺失时收益字段保持为空。
     */
    private BigDecimal changePercent(JsonNode response, String coinId, String vsCurrency) {
        JsonNode node = response == null ? null : response.path(coinId).path(vsCurrency + "_24h_change");
        return node != null && node.isNumber() ? node.decimalValue().setScale(4, RoundingMode.HALF_UP) : null;
    }

    /**
     * previous = price / (1 + changePercent / 100)，统一在后端完成行情口径推导。
     */
    private BigDecimal previousClose(BigDecimal price, BigDecimal changePercent) {
        if (price == null || changePercent == null) {
            return null;
        }
        BigDecimal divisor = BigDecimal.ONE.add(changePercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return divisor.compareTo(BigDecimal.ZERO) == 0 ? null : price.divide(divisor, 8, RoundingMode.HALF_UP);
    }
}
