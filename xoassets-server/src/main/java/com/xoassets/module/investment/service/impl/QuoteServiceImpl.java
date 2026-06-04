package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.module.investment.dto.ManualQuoteRequest;
import com.xoassets.module.investment.provider.QuoteFetchResult;
import com.xoassets.module.investment.provider.QuoteProvider;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.QuoteRawSnapshot;
import com.xoassets.module.investment.service.QuoteRawSnapshotService;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceMapper;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 行情价格服务实现：当前价落 MySQL current，原始快照短期落 Redis。
 */
@Slf4j
@Service
public class QuoteServiceImpl implements QuoteService {

    private static final LocalTime STOCK_REFRESH_START = LocalTime.of(9, 30);
    private static final LocalTime STOCK_REFRESH_END = LocalTime.of(15, 0);

    private final AssetPriceMapper assetPriceMapper;
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    private final QuoteRawSnapshotService quoteRawSnapshotService;
    private final AssetService assetService;
    private final List<QuoteProvider> quoteProviders;

    public QuoteServiceImpl(
            AssetPriceMapper assetPriceMapper,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            QuoteRawSnapshotService quoteRawSnapshotService,
            AssetService assetService,
            List<QuoteProvider> quoteProviders) {
        this.assetPriceMapper = assetPriceMapper;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.quoteRawSnapshotService = quoteRawSnapshotService;
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
        price.setMarketStatus("MANUAL");
        price.setRawJson(null);
        price.setDeleted(0);
        assetPriceMapper.insert(price);
        upsertCurrent(toCurrent(price));
        appendRawSnapshot(toRawSnapshot(price));
        return toVO(price);
    }

    /**
     * 用户手动刷新必须绕过 TTL 和交易时段缓存，尽量拿到最新可用收盘价。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO refreshQuote(Long assetId) {
        return refreshQuoteInternal(assetId, true);
    }

    /**
     * 批量刷新行情，单个资产失败时保留旧价格，避免影响整批持仓刷新。
     */
    @Override
    public List<AssetPriceVO> refreshQuotes(Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        List<AssetPriceVO> results = new ArrayList<>();
        for (Long assetId : assetIds.stream().distinct().toList()) {
            try {
                AssetPriceVO refreshed = refreshQuote(assetId);
                if (refreshed != null) {
                    results.add(refreshed);
                }
            } catch (Exception exception) {
                log.warn("批量刷新行情失败 assetId={}", assetId, exception);
                AssetPrice latestPrice = latestPrice(assetId);
                if (latestPrice != null) {
                    results.add(toVO(latestPrice));
                }
            }
        }
        return results;
    }

    /**
     * 刷新过期行情；手动价格永不过期，失败时不删除旧价格。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetPriceVO refreshQuoteIfStale(Long assetId) {
        return refreshQuoteInternal(assetId, false);
    }

    /**
     * force=true 用于用户主动刷新；force=false 用于定时任务和后台按需刷新。
     */
    private AssetPriceVO refreshQuoteInternal(Long assetId, boolean force) {
        Asset asset = assetService.findAsset(assetId);
        AssetPrice latestPrice = latestPrice(assetId);
        if (!force && isOutsideStockRefreshWindow(asset)) {
            // 股票行情只在 09:30-15:00 之间主动刷新；非交易时段直接复用最近快照，避免无意义写入。
            if (latestPrice != null) {
                return toVO(latestPrice);
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "股票行情仅在交易时段刷新");
        }
        if (!force && isFresh(asset, latestPrice)) {
            return toVO(latestPrice);
        }
        QuoteProvider provider = quoteProviders.stream()
                .filter(item -> item.supports(asset))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "当前资产暂不支持自动刷新行情"));
        QuoteFetchResult result;
        try {
            result = provider.fetch(asset);
        } catch (Exception exception) {
            // 第三方行情失败时保留旧价格；没有旧价格才把错误返回给前端，引导用户手动录价。
            log.warn("资产行情刷新失败，保留最近价格 assetId={}", assetId, exception);
            if (latestPrice != null) {
                return toVO(latestPrice);
            }
            throw exception;
        }
        AssetPrice price = toAssetPrice(asset.getId(), result);
        upsertCurrent(toCurrent(price));
        appendRawSnapshot(toRawSnapshot(price));
        return toVO(price);
    }

    /**
     * 批量查询当前价格；没有 current 的资产才回退旧快照表，兼容迁移前历史数据。
     */
    @Override
    public Map<Long, AssetPrice> latestPriceMap(Collection<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AssetPrice> result = new HashMap<>();
        assetPriceCurrentMapper.selectBatchIds(assetIds).stream()
                .map(this::toAssetPrice)
                .forEach(price -> result.put(price.getAssetId(), price));
        List<Long> missingAssetIds = assetIds.stream().filter(assetId -> !result.containsKey(assetId)).toList();
        if (missingAssetIds.isEmpty()) {
            return result;
        }
        assetPriceMapper.selectList(new LambdaQueryWrapper<AssetPrice>()
                        .in(AssetPrice::getAssetId, missingAssetIds)
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
        AssetPriceCurrent current = assetPriceCurrentMapper.selectById(assetId);
        if (current != null) {
            return toAssetPrice(current);
        }
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
        if ("MANUAL".equals(asset.getQuoteSource())) {
            return true;
        }
        Duration ttl = switch (asset.getType()) {
            case "CRYPTO" -> Duration.ofHours(1);
            case "STOCK" -> Duration.ofMinutes(15);
            case "FUND" -> Duration.ofDays(1);
            default -> Duration.ZERO;
        };
        return ttl.isZero() || !price.getQuoteTime().isBefore(LocalDateTime.now().minus(ttl));
    }

    /**
     * 股票只在 09:30-15:00 之间拉取第三方行情，其他时间保留最近快照。
     */
    private boolean isOutsideStockRefreshWindow(Asset asset) {
        if (!"STOCK".equals(asset.getType())) {
            return false;
        }
        LocalTime now = LocalTime.now();
        return now.isBefore(STOCK_REFRESH_START) || now.isAfter(STOCK_REFRESH_END);
    }

    /**
     * 当前价每个资产只保留一条，用于持仓估值和首页投资资产。
     */
    private void upsertCurrent(AssetPriceCurrent current) {
        if (assetPriceCurrentMapper.selectById(current.getAssetId()) == null) {
            assetPriceCurrentMapper.insert(current);
            return;
        }
        assetPriceCurrentMapper.updateById(current);
    }

    /**
     * Redis 只保存短期原始快照，收益计算不会从 Redis 直接读取。
     */
    private void appendRawSnapshot(QuoteRawSnapshot snapshot) {
        quoteRawSnapshotService.append(snapshot);
    }

    private AssetPrice toAssetPrice(Long assetId, QuoteFetchResult result) {
        AssetPrice price = new AssetPrice();
        price.setAssetId(assetId);
        price.setPrice(result.price().setScale(8, RoundingMode.HALF_UP));
        price.setCurrency(result.currency());
        price.setPreviousClose(result.previousClose() == null ? null : result.previousClose().setScale(8, RoundingMode.HALF_UP));
        price.setChangeAmount(result.changeAmount() == null ? null : result.changeAmount().setScale(8, RoundingMode.HALF_UP));
        price.setChangePercent(result.changePercent() == null ? null : result.changePercent().setScale(4, RoundingMode.HALF_UP));
        price.setSource(result.source());
        price.setQuoteTime(result.quoteTime());
        price.setMarketStatus(result.marketStatus());
        price.setRawJson(result.rawJson());
        price.setDeleted(0);
        return price;
    }

    private AssetPriceCurrent toCurrent(AssetPrice price) {
        AssetPriceCurrent current = new AssetPriceCurrent();
        current.setAssetId(price.getAssetId());
        current.setPrice(price.getPrice().setScale(8, RoundingMode.HALF_UP));
        current.setCurrency(price.getCurrency());
        current.setPreviousClose(price.getPreviousClose());
        current.setChangeAmount(price.getChangeAmount());
        current.setChangePercent(price.getChangePercent());
        current.setSource(price.getSource());
        current.setQuoteTime(price.getQuoteTime());
        current.setMarketStatus(price.getMarketStatus());
        current.setRawJson(price.getRawJson());
        return current;
    }

    private AssetPrice toAssetPrice(AssetPriceCurrent current) {
        AssetPrice price = new AssetPrice();
        price.setAssetId(current.getAssetId());
        price.setPrice(current.getPrice());
        price.setCurrency(current.getCurrency());
        price.setPreviousClose(current.getPreviousClose());
        price.setChangeAmount(current.getChangeAmount());
        price.setChangePercent(current.getChangePercent());
        price.setSource(current.getSource());
        price.setQuoteTime(current.getQuoteTime());
        price.setMarketStatus(current.getMarketStatus());
        price.setRawJson(current.getRawJson());
        price.setCreatedAt(current.getCreatedAt());
        price.setUpdatedAt(current.getUpdatedAt());
        price.setDeleted(0);
        return price;
    }

    private QuoteRawSnapshot toRawSnapshot(AssetPrice price) {
        return new QuoteRawSnapshot(
                price.getAssetId(),
                price.getPrice(),
                price.getCurrency(),
                price.getPreviousClose(),
                price.getChangeAmount(),
                price.getChangePercent(),
                price.getSource(),
                price.getQuoteTime(),
                price.getMarketStatus());
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
                .previousClose(price.getPreviousClose())
                .changeAmount(price.getChangeAmount())
                .changePercent(price.getChangePercent())
                .source(price.getSource())
                .quoteTime(price.getQuoteTime())
                .marketStatus(price.getMarketStatus())
                .build();
    }
}
