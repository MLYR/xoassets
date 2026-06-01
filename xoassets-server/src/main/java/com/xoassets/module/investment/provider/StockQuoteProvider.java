package com.xoassets.module.investment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.persistence.entity.Asset;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 股票行情 provider：A 股使用新浪行情，美股使用 Yahoo Finance chart。
 */
@Component
public class StockQuoteProvider implements QuoteProvider {

    private static final String ASSET_TYPE_STOCK = "STOCK";
    private static final String SOURCE_SINA = "SINA";
    private static final String SOURCE_YAHOO = "YAHOO";

    private final RestClient sinaClient;
    private final RestClient yahooClient;
    private final ObjectMapper objectMapper;

    public StockQuoteProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.sinaClient = RestClient.builder()
                .baseUrl("https://hq.sinajs.cn")
                .defaultHeader(HttpHeaders.REFERER, "https://finance.sina.com.cn")
                .build();
        this.yahooClient = RestClient.builder().baseUrl("https://query1.finance.yahoo.com").build();
    }

    @Override
    public boolean supports(Asset asset) {
        return ASSET_TYPE_STOCK.equals(asset.getType()) && (SOURCE_SINA.equals(asset.getQuoteSource()) || SOURCE_YAHOO.equals(asset.getQuoteSource()));
    }

    @Override
    public QuoteFetchResult fetch(Asset asset) {
        if (SOURCE_SINA.equals(asset.getQuoteSource())) {
            return fetchAshare(asset);
        }
        return fetchUs(asset);
    }

    /**
     * A 股 quoteKey 使用 600519.SH / 000001.SZ / 430047.BJ，转换为新浪 sh600519 等查询键。
     */
    private QuoteFetchResult fetchAshare(Asset asset) {
        String sinaKey = sinaKey(asset);
        try {
            byte[] bytes = sinaClient.get()
                    .uri("/list={key}", sinaKey)
                    .retrieve()
                    .body(byte[].class);
            String body = new String(bytes == null ? new byte[0] : bytes, java.nio.charset.Charset.forName("GBK"));
            String[] fields = quotePayload(body).split(",");
            if (fields.length < 32 || !StringUtils.hasText(fields[3])) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "A股行情未返回有效价格");
            }
            BigDecimal previousClose = new BigDecimal(fields[2]).setScale(8, RoundingMode.HALF_UP);
            BigDecimal price = new BigDecimal(fields[3]).setScale(8, RoundingMode.HALF_UP);
            BigDecimal changeAmount = price.subtract(previousClose).setScale(8, RoundingMode.HALF_UP);
            BigDecimal changePercent = previousClose.compareTo(BigDecimal.ZERO) == 0
                    ? null
                    : changeAmount.multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP);
            return new QuoteFetchResult(
                    price,
                    "CNY",
                    previousClose,
                    changeAmount,
                    changePercent,
                    SOURCE_SINA,
                    LocalDateTime.parse(fields[30] + "T" + fields[31]),
                    "UNKNOWN",
                    new String(bytes == null ? new byte[0] : bytes, StandardCharsets.ISO_8859_1));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "A股行情刷新失败");
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "A股行情解析失败");
        }
    }

    /**
     * 美股 quoteKey 使用 AAPL / MSFT 等 Yahoo 标准代码。
     */
    private QuoteFetchResult fetchUs(Asset asset) {
        String symbol = StringUtils.hasText(asset.getQuoteKey()) ? asset.getQuoteKey().trim().toUpperCase(Locale.ROOT) : asset.getSymbol();
        if (!StringUtils.hasText(symbol)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "美股行情键不能为空");
        }
        try {
            JsonNode response = yahooClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v8/finance/chart/{symbol}")
                            .queryParam("range", "1d")
                            .queryParam("interval", "1m")
                            .build(symbol))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode meta = response == null ? null : response.path("chart").path("result").path(0).path("meta");
            JsonNode priceNode = meta == null ? null : meta.path("regularMarketPrice");
            if (priceNode == null || !priceNode.isNumber()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "美股行情未返回有效价格");
            }
            BigDecimal price = priceNode.decimalValue().setScale(8, RoundingMode.HALF_UP);
            BigDecimal previousClose = meta.path("previousClose").isNumber() ? meta.path("previousClose").decimalValue().setScale(8, RoundingMode.HALF_UP) : null;
            BigDecimal changeAmount = previousClose == null ? null : price.subtract(previousClose).setScale(8, RoundingMode.HALF_UP);
            BigDecimal changePercent = previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0
                    ? null
                    : changeAmount.multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP);
            LocalDateTime quoteTime = meta.path("regularMarketTime").isNumber()
                    ? LocalDateTime.ofInstant(Instant.ofEpochSecond(meta.path("regularMarketTime").asLong()), ZoneId.systemDefault())
                    : LocalDateTime.now();
            return new QuoteFetchResult(
                    price,
                    meta.path("currency").asText("USD"),
                    previousClose,
                    changeAmount,
                    changePercent,
                    SOURCE_YAHOO,
                    quoteTime,
                    meta.path("marketState").asText("UNKNOWN"),
                    objectMapper.writeValueAsString(response));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "美股行情刷新失败");
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "美股行情解析失败");
        }
    }

    private String quotePayload(String body) {
        int start = body.indexOf('"');
        int end = body.lastIndexOf('"');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票行情格式不正确");
        }
        return body.substring(start + 1, end);
    }

    private String sinaKey(Asset asset) {
        String key = StringUtils.hasText(asset.getQuoteKey()) ? asset.getQuoteKey() : asset.getSymbol();
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "A股行情键不能为空");
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith(".SH")) {
            return "sh" + normalized.substring(0, normalized.length() - 3);
        }
        if (normalized.endsWith(".SZ")) {
            return "sz" + normalized.substring(0, normalized.length() - 3);
        }
        if (normalized.endsWith(".BJ")) {
            return "bj" + normalized.substring(0, normalized.length() - 3);
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "A股行情键请使用 600519.SH、000001.SZ 或 430047.BJ 格式");
    }
}
