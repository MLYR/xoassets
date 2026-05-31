package com.xoassets.module.investment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.persistence.entity.Asset;
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

    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";
    private static final String SOURCE_COINGECKO = "COINGECKO";
    private static final Map<String, String> SUPPORTED_COINS = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "SOL", "solana",
            "BNB", "binancecoin",
            "DOGE", "dogecoin");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CoinGeckoQuoteProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 使用 CoinGecko keyless public API，后续如需 Pro API 再抽配置。
        this.restClient = RestClient.builder().baseUrl("https://api.coingecko.com/api/v3").build();
    }

    @Override
    public boolean supports(Asset asset) {
        return ASSET_TYPE_CRYPTO.equals(asset.getType()) && SOURCE_COINGECKO.equals(asset.getQuoteSource());
    }

    @Override
    public QuoteFetchResult fetch(Asset asset) {
        String coinId = resolveCoinId(asset);
        String vsCurrency = StringUtils.hasText(asset.getCurrency()) ? asset.getCurrency().toLowerCase(Locale.ROOT) : "cny";
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/simple/price")
                            .queryParam("ids", coinId)
                            .queryParam("vs_currencies", vsCurrency)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode priceNode = response == null ? null : response.path(coinId).path(vsCurrency);
            if (priceNode == null || !priceNode.isNumber()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "CoinGecko 未返回有效价格");
            }
            return new QuoteFetchResult(
                    // 行情价格保留 8 位，避免 DOGE 等低单价币种展示价和市值计算口径不一致。
                    priceNode.decimalValue().setScale(8, java.math.RoundingMode.HALF_UP),
                    vsCurrency.toUpperCase(Locale.ROOT),
                    SOURCE_COINGECKO,
                    LocalDateTime.now(),
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
}
