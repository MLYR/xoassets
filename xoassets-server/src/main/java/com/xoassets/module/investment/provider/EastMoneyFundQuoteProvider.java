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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 天天基金公开净值 provider，基金当前价统一使用单位净值。
 */
@Slf4j
@Component
public class EastMoneyFundQuoteProvider implements QuoteProvider {

    /**
     * 基金资产类型常量。
     */
    private static final String ASSET_TYPE_FUND = "FUND";
    /**
     * 天天基金来源常量。
     */
    private static final String SOURCE_EASTMONEY = "EASTMONEY";
    /**
     * 历史净值行匹配规则。
     */
    private static final Pattern HISTORY_ROW_PATTERN = Pattern.compile("<tr><td>(\\d{4}-\\d{2}-\\d{2})</td><td[^>]*>([0-9.]+)</td><td[^>]*>[0-9.]+</td><td[^>]*>([-+0-9.]+)%</td>");

    /**
     * HTTP客户端。
     */
    private final RestClient restClient;
    /**
     * 历史净值HTTP客户端。
     */
    private final RestClient historyRestClient;
    /**
     * JSON序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 初始化行情提供方。
     */
    public EastMoneyFundQuoteProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl("https://fundgz.1234567.com.cn").build();
        this.historyRestClient = RestClient.builder().baseUrl("https://fundf10.eastmoney.com").build();
    }

    /**
     * 判断是否支持该资产。
     */
    @Override
    public boolean supports(Asset asset) {
        return ASSET_TYPE_FUND.equals(asset.getType()) && SOURCE_EASTMONEY.equals(asset.getQuoteSource());
    }

    /**
     * 拉取行情数据。
     */
    @Override
    public QuoteFetchResult fetch(Asset asset) {
        String fundCode = StringUtils.hasText(asset.getQuoteKey()) ? asset.getQuoteKey().trim() : asset.getSymbol();
        if (!StringUtils.hasText(fundCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "基金行情键不能为空");
        }
        try {
            List<FundNavRow> navRows = fetchHistoryRows(fundCode);
            if (!navRows.isEmpty()) {
                FundNavRow latest = navRows.get(0);
                BigDecimal previousClose = navRows.size() > 1 ? navRows.get(1).price() : null;
                BigDecimal changeAmount = previousClose == null ? null : latest.price().subtract(previousClose).setScale(8, RoundingMode.HALF_UP);
                // 基金昨价必须取上一交易日单位净值，不能用估算涨跌幅反推，否则持仓市值和昨收口径会不一致。
                return new QuoteFetchResult(
                        latest.price(),
                        "CNY",
                        previousClose,
                        changeAmount,
                        latest.changePercent(),
                        SOURCE_EASTMONEY,
                        LocalDateTime.of(latest.date(), LocalTime.of(15, 0)),
                        "CLOSED",
                        navRows.toString());
            }
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
     * 天天基金 F10 历史净值表返回最近净值列表，第一行最新、第二行上一交易日，用于修正基金昨价口径。
     */
    private List<FundNavRow> fetchHistoryRows(String fundCode) {
        try {
            byte[] bytes = historyRestClient.get()
                    .uri("/F10DataApi.aspx?type=lsjz&code={code}&page=1&per=5&sdate=&edate=&rt={timestamp}", fundCode, System.currentTimeMillis())
                    .retrieve()
                    .body(byte[].class);
            String body = new String(bytes == null ? new byte[0] : bytes, StandardCharsets.UTF_8);
            Matcher matcher = HISTORY_ROW_PATTERN.matcher(body);
            List<FundNavRow> rows = new ArrayList<>();
            while (matcher.find()) {
                rows.add(new FundNavRow(
                        LocalDate.parse(matcher.group(1)),
                        new BigDecimal(matcher.group(2)).setScale(8, RoundingMode.HALF_UP),
                        new BigDecimal(matcher.group(3)).setScale(4, RoundingMode.HALF_UP)));
            }
            return rows;
        } catch (Exception exception) {
            log.warn("基金历史净值查询失败，回退实时净值接口 source=EASTMONEY_F10, code={}", fundCode, exception);
            return List.of();
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
     * 净值日期没有时不能用当前时间兜底，否则会把无日期基金净值误判成今日有效价格。
     */
    private LocalDateTime quoteTime(String dateText) {
        if (!StringUtils.hasText(dateText)) {
            return LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT);
        }
        return LocalDateTime.of(LocalDate.parse(dateText), LocalTime.of(15, 0));
    }

    /**
     * 解析可为空金额。
     */
    private BigDecimal decimalOrNull(JsonNode node, int scale) {
        if (node == null || node.isMissingNode() || !StringUtils.hasText(node.asText())) {
            return null;
        }
        return new BigDecimal(node.asText()).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 根据涨跌幅反推上一交易日净值。
     */
    private BigDecimal previousClose(BigDecimal price, BigDecimal changePercent) {
        if (price == null || changePercent == null) {
            return null;
        }
        BigDecimal divisor = BigDecimal.ONE.add(changePercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return divisor.compareTo(BigDecimal.ZERO) == 0 ? null : price.divide(divisor, 8, RoundingMode.HALF_UP);
    }

    /**
     * 基金历史净值行。
     */
    private record FundNavRow(LocalDate date, BigDecimal price, BigDecimal changePercent) {
    }
}
