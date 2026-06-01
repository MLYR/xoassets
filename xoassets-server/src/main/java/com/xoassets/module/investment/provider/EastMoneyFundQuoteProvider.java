package com.xoassets.module.investment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.persistence.entity.Asset;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 天天基金公开净值 provider，基金当前价统一使用单位净值。
 */
@Component
public class EastMoneyFundQuoteProvider implements QuoteProvider {

    private static final String ASSET_TYPE_FUND = "FUND";
    private static final String SOURCE_EASTMONEY = "EASTMONEY";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EastMoneyFundQuoteProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl("https://fundgz.1234567.com.cn").build();
    }

    @Override
    public boolean supports(Asset asset) {
        return ASSET_TYPE_FUND.equals(asset.getType()) && SOURCE_EASTMONEY.equals(asset.getQuoteSource());
    }

    @Override
    public QuoteFetchResult fetch(Asset asset) {
        String fundCode = StringUtils.hasText(asset.getQuoteKey()) ? asset.getQuoteKey().trim() : asset.getSymbol();
        if (!StringUtils.hasText(fundCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "基金行情键不能为空");
        }
        try {
            byte[] bytes = restClient.get()
                    .uri("/js/{code}.js?rt={timestamp}", fundCode, System.currentTimeMillis())
                    .retrieve()
                    .body(byte[].class);
            // 天天基金接口返回 UTF-8 JSONP，按 String.class 读取时可能被默认编码误解导致中文基金名乱码。
            String body = new String(bytes == null ? new byte[0] : bytes, StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(jsonBody(body));
            JsonNode navNode = node.path("dwjz");
            if (!navNode.isTextual() || !StringUtils.hasText(navNode.asText())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "基金行情未返回有效单位净值");
            }
            BigDecimal price = new BigDecimal(navNode.asText()).setScale(8, RoundingMode.HALF_UP);
            BigDecimal changePercent = decimalOrNull(node.path("gszzl"), 4);
            BigDecimal previousClose = previousClose(price, changePercent);
            BigDecimal changeAmount = previousClose == null ? null : price.subtract(previousClose).setScale(8, RoundingMode.HALF_UP);
            return new QuoteFetchResult(
                    price,
                    "CNY",
                    previousClose,
                    changeAmount,
                    changePercent,
                    SOURCE_EASTMONEY,
                    quoteTime(node.path("jzrq").asText(null)),
                    "CLOSED",
                    objectMapper.writeValueAsString(node));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "基金行情刷新失败");
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "基金行情解析失败");
        }
    }

    /**
     * 天天基金返回 jsonpgz(...) 包装，这里只提取中间 JSON。
     */
    private String jsonBody(String body) {
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "基金行情响应为空");
        }
        int start = body.indexOf('{');
        int end = body.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "基金行情格式不正确");
        }
        return body.substring(start, end + 1);
    }

    /**
     * 净值日期没有时用当前时间兜底，避免缺少日期导致刷新失败。
     */
    private LocalDateTime quoteTime(String dateText) {
        if (!StringUtils.hasText(dateText)) {
            return LocalDateTime.now();
        }
        return LocalDateTime.of(LocalDate.parse(dateText), LocalTime.of(15, 0));
    }

    private BigDecimal decimalOrNull(JsonNode node, int scale) {
        if (node == null || node.isMissingNode() || !StringUtils.hasText(node.asText())) {
            return null;
        }
        return new BigDecimal(node.asText()).setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal previousClose(BigDecimal price, BigDecimal changePercent) {
        if (price == null || changePercent == null) {
            return null;
        }
        BigDecimal divisor = BigDecimal.ONE.add(changePercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return divisor.compareTo(BigDecimal.ZERO) == 0 ? null : price.divide(divisor, 8, RoundingMode.HALF_UP);
    }
}
