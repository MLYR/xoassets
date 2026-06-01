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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 公共资产服务实现。
 */
@Service
public class AssetServiceImpl implements AssetService {

    private static final List<String> ASSET_TYPES = List.of("STOCK", "FUND", "CRYPTO", "OTHER");
    private static final List<String> QUOTE_SOURCES = List.of("MANUAL", "COINGECKO", "EASTMONEY", "SINA", "YAHOO", "ALPHA_VANTAGE", "TUSHARE", "AKSHARE");
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
    private static final Map<String, String> COIN_SYMBOLS = Map.of(
            "bitcoin", "BTC",
            "ethereum", "ETH",
            "solana", "SOL",
            "binancecoin", "BNB",
            "dogecoin", "DOGE");
    private static final Map<String, String> COIN_NAMES = Map.of(
            "bitcoin", "Bitcoin",
            "ethereum", "Ethereum",
            "solana", "Solana",
            "binancecoin", "BNB",
            "dogecoin", "Dogecoin");

    private final AssetMapper assetMapper;
    private final ObjectMapper objectMapper;
    private final RestClient coinGeckoClient;
    private final RestClient fundClient;
    private final RestClient sinaClient;
    private final RestClient yahooClient;

    public AssetServiceImpl(AssetMapper assetMapper, ObjectMapper objectMapper) {
        this.assetMapper = assetMapper;
        this.objectMapper = objectMapper;
        this.coinGeckoClient = RestClient.builder().baseUrl("https://api.coingecko.com/api/v3").build();
        this.fundClient = RestClient.builder().baseUrl("https://fundgz.1234567.com.cn").build();
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
            byte[] bytes = fundClient.get()
                    .uri("/js/{code}.js?rt={timestamp}", code, System.currentTimeMillis())
                    .retrieve()
                    .body(byte[].class);
            // 天天基金接口返回 UTF-8 JSONP，显式按 UTF-8 解码，避免基金名称变成乱码。
            String body = new String(bytes == null ? new byte[0] : bytes, StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(jsonBody(body));
            BigDecimal price = decimal(node.path("dwjz"), 8);
            BigDecimal changePercent = decimalOrNull(node.path("gszzl"), 4);
            return List.of(AssetLookupVO.builder()
                    .name(node.path("name").asText(code))
                    .symbol(code)
                    .assetType("FUND")
                    .market("CN_FUND")
                    .currency("CNY")
                    .quoteSource("EASTMONEY")
                    .quoteKey(code)
                    .latestPrice(price)
                    .previousClose(previousClose(price, changePercent))
                    .changePercent(changePercent)
                    .quoteTime(fundQuoteTime(node.path("jzrq").asText(null)))
                    .build());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
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

    private List<AssetLookupVO> lookupAshare(String keyword, String market) {
        String code = keyword.trim().toUpperCase(Locale.ROOT).replace("." + market, "");
        String sinaKey = market.toLowerCase(Locale.ROOT) + code;
        try {
            byte[] bytes = sinaClient.get().uri("/list={key}", sinaKey).retrieve().body(byte[].class);
            String body = new String(bytes == null ? new byte[0] : bytes, Charset.forName("GBK"));
            String[] fields = quotePayload(body).split(",");
            if (fields.length < 32 || !StringUtils.hasText(fields[3])) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票资产信息查询失败");
            }
            BigDecimal previousClose = new BigDecimal(fields[2]).setScale(8, RoundingMode.HALF_UP);
            BigDecimal price = new BigDecimal(fields[3]).setScale(8, RoundingMode.HALF_UP);
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
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票资产信息查询失败");
        }
    }

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
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票资产信息查询失败");
        }
    }

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

    private String jsonBody(String body) {
        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "行情响应为空");
        }
        int start = body.indexOf('{');
        int end = body.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "行情格式不正确");
        }
        return body.substring(start, end + 1);
    }

    private String quotePayload(String body) {
        int start = body.indexOf('"');
        int end = body.lastIndexOf('"');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票行情格式不正确");
        }
        return body.substring(start + 1, end);
    }

    private LocalDateTime fundQuoteTime(String dateText) {
        return StringUtils.hasText(dateText) ? LocalDateTime.of(LocalDate.parse(dateText), LocalTime.of(15, 0)) : LocalDateTime.now();
    }

    private BigDecimal decimal(JsonNode node, int scale) {
        if (node == null || node.isMissingNode() || !StringUtils.hasText(node.asText())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "行情未返回有效价格");
        }
        return new BigDecimal(node.asText()).setScale(scale, RoundingMode.HALF_UP);
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

    private BigDecimal changePercent(BigDecimal price, BigDecimal previousClose) {
        if (price == null || previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return price.subtract(previousClose).multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP);
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
