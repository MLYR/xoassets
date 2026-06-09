package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.module.investment.dto.AssetRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.vo.AssetLookupVO;
import com.xoassets.module.investment.vo.AssetVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.mapper.AssetMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 公共资产服务实现。
 */
@Slf4j
@Service
public class AssetServiceImpl implements AssetService {

    /**
     * 支持的资产类型列表。
     */
    private static final List<String> ASSET_TYPES = List.of("STOCK", "FUND", "CRYPTO", "OTHER");
    /**
     * 支持的行情来源列表。
     */
    private static final List<String> QUOTE_SOURCES = List.of("MANUAL", "COINGECKO", "EASTMONEY", "SINA", "YAHOO", "ALPHA_VANTAGE", "TUSHARE", "AKSHARE");
    /**
     * 虚拟货币代码到 CoinGecko ID 的映射。
     */
    private static final Map<String, String> COIN_IDS = Map.of(
            "BTC", "bitcoin",
            "BITCOIN", "bitcoin",
            "ETH", "ethereum",
            "ETHEREUM", "ethereum",
            "SOL", "solana",
            "SOLANA", "solana",
            "BNB", "binancecoin",
            "DOGE", "dogecoin",
            "DOGECOIN", "dogecoin");
    /**
     * CoinGecko ID 到展示代码的映射。
     */
    private static final Map<String, String> COIN_SYMBOLS = Map.of(
            "bitcoin", "BTC",
            "ethereum", "ETH",
            "solana", "SOL",
            "binancecoin", "BNB",
            "dogecoin", "DOGE");
    /**
     * CoinGecko ID 到展示名称的映射。
     */
    private static final Map<String, String> COIN_NAMES = Map.of(
            "bitcoin", "Bitcoin",
            "ethereum", "Ethereum",
            "solana", "Solana",
            "binancecoin", "BNB",
            "dogecoin", "Dogecoin");
    /**
     * 基金历史净值行匹配规则。
     */
    private static final Pattern FUND_HISTORY_ROW_PATTERN = Pattern.compile("<tr><td>(\\d{4}-\\d{2}-\\d{2})</td><td[^>]*>([0-9.]+)</td><td[^>]*>[0-9.]+</td><td[^>]*>([-+0-9.]+)%</td>");

    /**
     * 资产数据访问组件。
     */
    private final AssetMapper assetMapper;
    /**
     * JSON序列化组件。
     */
    private final ObjectMapper objectMapper;
    /**
     * CoinGecko行情HTTP客户端。
     */
    private final RestClient coinGeckoClient;
    /**
     * 天天基金实时行情HTTP客户端。
     */
    private final RestClient fundClient;
    /**
     * 天天基金历史净值HTTP客户端。
     */
    private final RestClient fundHistoryClient;
    /**
     * 新浪行情HTTP客户端。
     */
    private final RestClient sinaClient;
    /**
     * 雅虎行情HTTP客户端。
     */
    private final RestClient yahooClient;

    /**
     * 注入业务依赖。
     */
    public AssetServiceImpl(AssetMapper assetMapper, ObjectMapper objectMapper) {
        this.assetMapper = assetMapper;
        this.objectMapper = objectMapper;
        this.coinGeckoClient = RestClient.builder().baseUrl("https://api.coingecko.com/api/v3").build();
        this.fundClient = RestClient.builder().baseUrl("https://fundgz.1234567.com.cn").build();
        this.fundHistoryClient = RestClient.builder().baseUrl("https://fundf10.eastmoney.com").build();
        this.sinaClient = RestClient.builder()
                .baseUrl("https://hq.sinajs.cn")
                .defaultHeader(HttpHeaders.REFERER, "https://finance.sina.com.cn")
                .build();
        this.yahooClient = RestClient.builder().baseUrl("https://query1.finance.yahoo.com").build();
    }

    /**
     * 按关键词和类型搜索公共资产；资产表不带 user_id。
     */
    @Override
    public List<AssetVO> search(String keyword, String type) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .eq(Asset::getStatus, 1)
                .orderByDesc(Asset::getCreatedAt);
        if (StringUtils.hasText(type)) {
            ensureAssetType(type);
            wrapper.eq(Asset::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Asset::getName, keyword).or().like(Asset::getSymbol, keyword));
        }
        return assetMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /**
     * 自动识别资产信息；第三方接口失败时抛出业务异常，前端可继续手动录入。
     */
    @Override
    public List<AssetLookupVO> lookup(String type, String keyword, String market) {
        ensureAssetType(type);
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入代码或名称");
        }
        return switch (type) {
            case "CRYPTO" -> lookupCrypto(keyword);
            case "FUND" -> lookupFund(keyword);
            case "STOCK" -> lookupStock(keyword, market);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "该资产类型暂不支持自动识别");
        };
    }

    /**
     * 创建公共资产，按类型、市场和代码防止重复。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetVO create(AssetRequest request) {
        ensureAssetType(request.getType());
        ensureQuoteSource(request.getQuoteSource());
        String market = normalizeMarket(request.getType(), request.getMarket(), request.getSymbol());
        Long exists = assetMapper.selectCount(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getType, request.getType())
                .eq(Asset::getMarket, market)
                .eq(Asset::getSymbol, request.getSymbol()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "资产已存在");
        }
        Asset asset = new Asset();
        asset.setSymbol(request.getSymbol());
        asset.setName(request.getName());
        asset.setType(request.getType());
        asset.setMarket(market);
        asset.setCurrency(request.getCurrency());
        asset.setQuoteSource(request.getQuoteSource());
        asset.setQuoteKey(request.getQuoteKey());
        asset.setStatus(1);
        asset.setDeleted(0);
        assetMapper.insert(asset);
        return toVO(asset);
    }

    /**
     * 查询公共资产，不存在或停用时按不存在处理。
     */
    @Override
    public Asset findAsset(Long id) {
        Asset asset = assetMapper.selectOne(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getId, id)
                .eq(Asset::getStatus, 1));
        if (asset == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资产不存在");
        }
        return asset;
    }

    /**
     * 资产类型白名单校验。
     */
    private void ensureAssetType(String type) {
        if (!ASSET_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资产类型只支持 STOCK、FUND、CRYPTO、OTHER");
        }
    }

    /**
     * 行情来源白名单校验。
     */
    private void ensureQuoteSource(String source) {
        if (!QUOTE_SOURCES.contains(source)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "行情来源不支持");
        }
    }

    /**
     * 虚拟货币只开放已接入 CoinGecko 的常用币，避免前端拼接第三方参数。
     */
    private List<AssetLookupVO> lookupCrypto(String keyword) {
        String normalized = keyword.trim().toUpperCase(Locale.ROOT);
        String coinId = COIN_IDS.getOrDefault(normalized, keyword.trim().toLowerCase(Locale.ROOT));
        if (!COIN_SYMBOLS.containsKey(coinId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "虚拟货币暂只支持 BTC、ETH、SOL、BNB、DOGE");
        }
        try {
            JsonNode response = coinGeckoClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/simple/price")
                            .queryParam("ids", coinId)
                            .queryParam("vs_currencies", "usd,cny")
                            .queryParam("include_24hr_change", "true")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode node = response == null ? null : response.path(coinId);
            BigDecimal price = decimal(node == null ? null : node.path("usd"), 8);
            BigDecimal changePercent = decimalOrNull(node == null ? null : node.path("usd_24h_change"), 4);
            return List.of(AssetLookupVO.builder()
                    .name(COIN_NAMES.get(coinId))
                    .symbol(COIN_SYMBOLS.get(coinId))
                    .assetType("CRYPTO")
                    .market("CRYPTO")
                    .currency("USD")
                    .quoteSource("COINGECKO")
                    .quoteKey(coinId)
                    .latestPrice(price)
                    .previousClose(previousClose(price, changePercent))
                    .changePercent(changePercent)
                    .quoteTime(LocalDateTime.now())
                    .build());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("虚拟货币资产信息查询失败 source=COINGECKO, keyword={}, coinId={}, message={}",
                    safeLog(keyword), safeLog(coinId), exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "虚拟货币资产信息查询失败");
        }
    }

    /**
     * 基金使用天天基金净值接口，当前价使用单位净值。
     */
    private List<AssetLookupVO> lookupFund(String keyword) {
        String code = keyword.trim();
        if (!code.matches("\\d{6}")) {
            return existingLookup("FUND", keyword);
        }
        try {
            List<FundNavRow> navRows = fetchFundHistoryRows(code);
            byte[] bytes = fundClient.get()
                    .uri("/js/{code}.js?rt={timestamp}", code, System.currentTimeMillis())
                    .retrieve()
                    .body(byte[].class);
            // 天天基金接口返回 UTF-8 JSONP，显式按 UTF-8 解码，避免基金名称变成乱码。
            String body = new String(bytes == null ? new byte[0] : bytes, StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(jsonBody(body, "EASTMONEY", code));
            if (!StringUtils.hasText(node.path("dwjz").asText(null))) {
                log.warn("基金资产信息查询缺少单位净值 source=EASTMONEY, code={}, response={}", safeLog(code), abbreviate(body));
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "基金资产信息查询失败");
            }
            // lookup 同样优先使用历史净值表的最新/上一交易日净值，避免保存持仓时带入估算昨价。
            BigDecimal price = navRows.isEmpty() ? decimal(node.path("dwjz"), 8) : navRows.get(0).price();
            BigDecimal previousClose = navRows.size() > 1 ? navRows.get(1).price() : previousClose(price, decimalOrNull(node.path("gszzl"), 4));
            BigDecimal changePercent = navRows.isEmpty() ? decimalOrNull(node.path("gszzl"), 4) : navRows.get(0).changePercent();
            return List.of(AssetLookupVO.builder()
                    .name(node.path("name").asText(code))
                    .symbol(code)
                    .assetType("FUND")
                    .market("CN_FUND")
                    .currency("CNY")
                    .quoteSource("EASTMONEY")
                    .quoteKey(code)
                    .latestPrice(price)
                    .previousClose(previousClose)
                    .changePercent(changePercent)
                    .quoteTime(navRows.isEmpty() ? fundQuoteTime(node.path("jzrq").asText(null)) : LocalDateTime.of(navRows.get(0).date(), LocalTime.of(15, 0)))
                    .build());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("基金资产信息查询失败 source=EASTMONEY, code={}, message={}",
                    safeLog(code), exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "基金资产信息查询失败");
        }
    }

    /**
     * 股票按市场选择 A 股或美股查询；没有市场时根据代码做保守推断。
     */
    private List<AssetLookupVO> lookupStock(String keyword, String market) {
        String resolvedMarket = StringUtils.hasText(market) ? market.trim().toUpperCase(Locale.ROOT) : inferMarket(keyword);
        if ("US".equals(resolvedMarket)) {
            return lookupUsStock(keyword);
        }
        if (List.of("SH", "SZ", "BJ").contains(resolvedMarket)) {
            return lookupAshare(keyword, resolvedMarket);
        }
        return existingLookup("STOCK", keyword);
    }

    /**
     * 识别A股资产。
     */
    private List<AssetLookupVO> lookupAshare(String keyword, String market) {
        String code = keyword.trim().toUpperCase(Locale.ROOT).replace("." + market, "");
        String sinaKey = market.toLowerCase(Locale.ROOT) + code;
        try {
            byte[] bytes = sinaClient.get().uri("/list={key}", sinaKey).retrieve().body(byte[].class);
            String body = new String(bytes == null ? new byte[0] : bytes, Charset.forName("GBK"));
            String[] fields = quotePayload(body).split(",");
            if (fields.length < 32 || !StringUtils.hasText(fields[3])) {
                log.warn("A股资产信息查询响应无效 source=SINA, keyword={}, market={}, sinaKey={}, response={}",
                        safeLog(keyword), safeLog(market), safeLog(sinaKey), abbreviate(body));
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票资产信息查询失败");
            }
            BigDecimal previousClose = new BigDecimal(fields[2]).setScale(8, RoundingMode.HALF_UP);
            BigDecimal price = new BigDecimal(fields[3]).setScale(8, RoundingMode.HALF_UP);
            if (price.compareTo(BigDecimal.ZERO) <= 0 || previousClose.compareTo(BigDecimal.ZERO) <= 0) {
                // 新浪无效行情会返回 0，资产识别阶段也不能把它当作初始最新价。
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票资产信息查询失败");
            }
            return List.of(AssetLookupVO.builder()
                    .name(StringUtils.hasText(fields[0]) ? fields[0] : code)
                    .symbol(code + "." + market)
                    .assetType("STOCK")
                    .market(market)
                    .currency("CNY")
                    .quoteSource("SINA")
                    .quoteKey(code + "." + market)
                    .latestPrice(price)
                    .previousClose(previousClose)
                    .changePercent(changePercent(price, previousClose))
                    .quoteTime(LocalDateTime.parse(fields[30] + "T" + fields[31]))
                    .build());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("A股资产信息查询失败 source=SINA, keyword={}, market={}, sinaKey={}, message={}",
                    safeLog(keyword), safeLog(market), safeLog(sinaKey), exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票资产信息查询失败");
        }
    }

    /**
     * 识别美股资产。
     */
    private List<AssetLookupVO> lookupUsStock(String keyword) {
        String symbol = keyword.trim().toUpperCase(Locale.ROOT);
        try {
            JsonNode response = yahooClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v8/finance/chart/{symbol}")
                            .queryParam("range", "1d")
                            .queryParam("interval", "1m")
                            .build(symbol))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode meta = response == null ? null : response.path("chart").path("result").path(0).path("meta");
            BigDecimal price = decimal(meta == null ? null : meta.path("regularMarketPrice"), 8);
            BigDecimal previousClose = decimalOrNull(meta == null ? null : meta.path("previousClose"), 8);
            return List.of(AssetLookupVO.builder()
                    .name(meta == null ? symbol : meta.path("symbol").asText(symbol))
                    .symbol(symbol)
                    .assetType("STOCK")
                    .market("US")
                    .currency(meta == null ? "USD" : meta.path("currency").asText("USD"))
                    .quoteSource("YAHOO")
                    .quoteKey(symbol)
                    .latestPrice(price)
                    .previousClose(previousClose)
                    .changePercent(changePercent(price, previousClose))
                    .quoteTime(meta != null && meta.path("regularMarketTime").isNumber()
                            ? LocalDateTime.ofInstant(Instant.ofEpochSecond(meta.path("regularMarketTime").asLong()), ZoneId.systemDefault())
                            : LocalDateTime.now())
                    .build());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("美股资产信息查询失败 source=YAHOO, keyword={}, symbol={}, message={}",
                    safeLog(keyword), safeLog(symbol), exception.getMessage(), exception);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票资产信息查询失败");
        }
    }

    /**
     * 从已有公共资产中构造识别结果。
     */
    private List<AssetLookupVO> existingLookup(String type, String keyword) {
        List<AssetLookupVO> rows = search(keyword, type).stream()
                .map(asset -> AssetLookupVO.builder()
                        .name(asset.getName())
                        .symbol(asset.getSymbol())
                        .assetType(asset.getType())
                        .market(asset.getMarket())
                        .currency(asset.getCurrency())
                        .quoteSource(asset.getQuoteSource())
                        .quoteKey(asset.getQuoteKey())
                        .build())
                .toList();
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "资产信息查询失败，可手动录入");
        }
        return rows;
    }

    /**
     * 推断交易市场。
     */
    private String inferMarket(String keyword) {
        String normalized = keyword.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith(".SH")) return "SH";
        if (normalized.endsWith(".SZ")) return "SZ";
        if (normalized.endsWith(".BJ")) return "BJ";
        if (normalized.matches("\\d{6}")) {
            if (normalized.startsWith("6") || normalized.startsWith("9")) return "SH";
            if (normalized.startsWith("4") || normalized.startsWith("8")) return "BJ";
            return "SZ";
        }
        return "US";
    }

    /**
     * 资产市场统一落库，避免同代码但不同市场的股票互相覆盖。
     */
    private String normalizeMarket(String type, String market, String symbol) {
        if (StringUtils.hasText(market)) {
            return market.trim().toUpperCase(Locale.ROOT);
        }
        if ("CRYPTO".equals(type)) {
            return "CRYPTO";
        }
        if ("FUND".equals(type)) {
            return "CN_FUND";
        }
        if ("STOCK".equals(type) && StringUtils.hasText(symbol)) {
            return inferMarket(symbol);
        }
        return "UNKNOWN";
    }

    /**
     * 解析JSON响应。
     */
    private String jsonBody(String body) {
        return jsonBody(body, "UNKNOWN", null);
    }

    /**
     * 从 JSONP 响应中截取 JSON，并在失败时记录来源和响应摘要。
     */
    private String jsonBody(String body, String source, String key) {
        if (!StringUtils.hasText(body)) {
            log.warn("行情响应为空 source={}, key={}", source, safeLog(key));
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "行情响应为空");
        }
        int start = body.indexOf('{');
        int end = body.lastIndexOf('}');
        if (start < 0 || end <= start) {
            log.warn("行情格式不正确 source={}, key={}, response={}", source, safeLog(key), abbreviate(body));
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "行情格式不正确");
        }
        return body.substring(start, end + 1);
    }

    /**
     * 构建行情响应数据。
     */
    private String quotePayload(String body) {
        int start = body.indexOf('"');
        int end = body.lastIndexOf('"');
        if (start < 0 || end <= start) {
            log.warn("股票行情响应格式不正确 response={}", abbreviate(body));
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票行情格式不正确");
        }
        return body.substring(start + 1, end);
    }

    /**
     * 解析基金报价时间。
     */
    private LocalDateTime fundQuoteTime(String dateText) {
        return StringUtils.hasText(dateText) ? LocalDateTime.of(LocalDate.parse(dateText), LocalTime.of(15, 0)) : LocalDateTime.now();
    }

    /**
     * 天天基金 F10 历史净值表用于拿真实上一交易日净值；接口失败时交给外层保留清晰错误日志。
     */
    private List<FundNavRow> fetchFundHistoryRows(String code) {
        try {
            byte[] bytes = fundHistoryClient.get()
                    .uri("/F10DataApi.aspx?type=lsjz&code={code}&page=1&per=5&sdate=&edate=&rt={timestamp}", code, System.currentTimeMillis())
                    .retrieve()
                    .body(byte[].class);
            String body = new String(bytes == null ? new byte[0] : bytes, StandardCharsets.UTF_8);
            Matcher matcher = FUND_HISTORY_ROW_PATTERN.matcher(body);
            List<FundNavRow> rows = new ArrayList<>();
            while (matcher.find()) {
                rows.add(new FundNavRow(
                        LocalDate.parse(matcher.group(1)),
                        new BigDecimal(matcher.group(2)).setScale(8, RoundingMode.HALF_UP),
                        new BigDecimal(matcher.group(3)).setScale(4, RoundingMode.HALF_UP)));
            }
            return rows;
        } catch (Exception exception) {
            log.warn("基金历史净值查询失败，回退实时净值接口 source=EASTMONEY_F10, code={}", safeLog(code), exception);
            return List.of();
        }
    }

    /**
     * 解析金额。
     */
    private BigDecimal decimal(JsonNode node, int scale) {
        if (node == null || node.isMissingNode() || !StringUtils.hasText(node.asText())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "行情未返回有效价格");
        }
        return new BigDecimal(node.asText()).setScale(scale, RoundingMode.HALF_UP);
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
     * 根据涨跌幅反推上一交易日价格。
     */
    private BigDecimal previousClose(BigDecimal price, BigDecimal changePercent) {
        if (price == null || changePercent == null) {
            return null;
        }
        BigDecimal divisor = BigDecimal.ONE.add(changePercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return divisor.compareTo(BigDecimal.ZERO) == 0 ? null : price.divide(divisor, 8, RoundingMode.HALF_UP);
    }

    /**
     * 根据当前价和昨收价计算涨跌幅。
     */
    private BigDecimal changePercent(BigDecimal price, BigDecimal previousClose) {
        if (price == null || previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return price.subtract(previousClose).multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP);
    }

    /**
     * 基金历史净值行。
     */
    private record FundNavRow(LocalDate date, BigDecimal price, BigDecimal changePercent) {
    }

    /**
     * 日志中只保留排查必要的短文本，避免第三方响应过长刷屏。
     */
    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "..." : compact;
    }

    /**
     * 外部输入写日志前做长度限制，避免异常日志被超长参数污染。
     */
    private String safeLog(String value) {
        return abbreviate(value);
    }

    /**
     * 转换资产展示对象。
     */
    private AssetVO toVO(Asset asset) {
        return AssetVO.builder()
                .id(asset.getId())
                .symbol(asset.getSymbol())
                .name(asset.getName())
                .type(asset.getType())
                .market(asset.getMarket())
                .currency(asset.getCurrency())
                .quoteSource(asset.getQuoteSource())
                .quoteKey(asset.getQuoteKey())
                .build();
    }
}
