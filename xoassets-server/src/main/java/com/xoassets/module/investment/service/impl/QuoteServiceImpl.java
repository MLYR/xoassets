package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.module.investment.dto.ManualQuoteRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.mapper.AssetPriceMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行情价格服务实现，阶段一只支持手动报价。
 */
@Service
public class QuoteServiceImpl implements QuoteService {

    private final AssetPriceMapper assetPriceMapper;
    private final AssetService assetService;

    public QuoteServiceImpl(AssetPriceMapper assetPriceMapper, AssetService assetService) {
        this.assetPriceMapper = assetPriceMapper;
        this.assetService = assetService;
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
        price.setPrice(request.getPrice());
        price.setCurrency(request.getCurrency() == null ? asset.getCurrency() : request.getCurrency());
        price.setSource("MANUAL");
        price.setQuoteTime(request.getQuoteTime() == null ? LocalDateTime.now() : request.getQuoteTime());
        price.setRawJson(null);
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
