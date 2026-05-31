package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.module.investment.dto.ManualQuoteRequest;
import com.xoassets.module.investment.provider.QuoteFetchResult;
import com.xoassets.module.investment.provider.QuoteProvider;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.mapper.AssetPriceMapper;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 行情价格服务实现，阶段一只支持手动报价。
 */
@Service
public class QuoteServiceImpl implements QuoteService {

    private final AssetPriceMapper assetPriceMapper;
    private final AssetService assetService;
    private final List<QuoteProvider> quoteProviders;

    public QuoteServiceImpl(AssetPriceMapper assetPriceMapper, AssetService assetService, List<QuoteProvider> quoteProviders) {
        this.assetPriceMapper = assetPriceMapper;
        this.assetService = assetService;
        this.quoteProviders = quoteProviders;
    }

    /**
     * 手动录入价格时写入 xo_asset_price，资产价格表不带 user_id。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO manualQuote(ManualQuoteRequest request) {
        Asset asset = assetService.findAsset(request.getAssetId());
        AssetPrice price = new AssetPrice();
        price.setAssetId(asset.getId());
        // 行情价格按 8 位入库；持仓市值再按业务金额口径收敛到 4 位。
        price.setPrice(request.getPrice().setScale(8, RoundingMode.HALF_UP));
        price.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : asset.getCurrency());
        price.setSource("MANUAL");
        price.setQuoteTime(request.getQuoteTime() == null ? LocalDateTime.now() : request.getQuoteTime());
        price.setRawJson(null);
        price.setDeleted(0);
        assetPriceMapper.insert(price);
        return toVO(price);
    }

    /**
     * 按资产选择行情 provider；最近价格仍新鲜时直接复用，避免频繁请求第三方。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO refreshQuote(Long assetId) {
        return refreshQuoteIfStale(assetId);
    }

    /**
     * 刷新过期行情；手动价格永不过期，失败时不删除旧价格。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO refreshQuoteIfStale(Long assetId) {
        Asset asset = assetService.findAsset(assetId);
        AssetPrice latestPrice = latestPrice(assetId);
        if (isFresh(asset, latestPrice)) {
            return toVO(latestPrice);
        }
        QuoteProvider provider = quoteProviders.stream()
                .filter(item -> item.supports(asset))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "当前资产暂不支持自动刷新行情"));
        QuoteFetchResult result = provider.fetch(asset);
        AssetPrice price = new AssetPrice();
        price.setAssetId(asset.getId());
        price.setPrice(result.price());
        price.setCurrency(result.currency());
        price.setSource(result.source());
        price.setQuoteTime(result.quoteTime());
        price.setRawJson(result.rawJson());
        price.setDeleted(0);
        assetPriceMapper.insert(price);
        return toVO(price);
    }

    /**
     * 批量查询最近价格；没有价格的资产不放入结果，持仓估值自行兜底。
     */
    @Override
    public Map<Long, AssetPrice> latestPriceMap(Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AssetPrice> result = new HashMap<>();
        assetPriceMapper.selectList(new LambdaQueryWrapper<AssetPrice>()
                        .in(AssetPrice::getAssetId, assetIds)
                        .orderByDesc(AssetPrice::getQuoteTime)
                        .orderByDesc(AssetPrice::getCreatedAt))
                .stream()
                .sorted(Comparator.comparing(AssetPrice::getQuoteTime).reversed())
                .forEach(price -> result.putIfAbsent(price.getAssetId(), price));
        return result;
    }

    /**
     * 查询单个资产最近价格，供缓存判断和手动资产兜底使用。
     */
    private AssetPrice latestPrice(Long assetId) {
        return assetPriceMapper.selectOne(new LambdaQueryWrapper<AssetPrice>()
                .eq(AssetPrice::getAssetId, assetId)
                .orderByDesc(AssetPrice::getQuoteTime)
                .orderByDesc(AssetPrice::getCreatedAt)
                .last("LIMIT 1"));
    }

    /**
     * 按资产类型判断最近价格是否仍可复用。
     */
    private boolean isFresh(Asset asset, AssetPrice price) {
        if (price == null) {
            return false;
        }
        if ("MANUAL".equals(asset.getQuoteSource()) || "MANUAL".equals(price.getSource())) {
            return true;
        }
        Duration ttl = switch (asset.getType()) {
            case "CRYPTO" -> Duration.ofMinutes(5);
            case "STOCK" -> Duration.ofMinutes(15);
            case "FUND" -> Duration.ofDays(1);
            default -> Duration.ZERO;
        };
        return ttl.isZero() || !price.getQuoteTime().isBefore(LocalDateTime.now().minus(ttl));
    }

    /**
     * 转换价格快照展示对象。
     */
    private AssetPriceVO toVO(AssetPrice price) {
        return AssetPriceVO.builder()
                .id(price.getId())
                .assetId(price.getAssetId())
                .price(price.getPrice())
                .currency(price.getCurrency())
                .source(price.getSource())
                .quoteTime(price.getQuoteTime())
                .build();
    }
}
