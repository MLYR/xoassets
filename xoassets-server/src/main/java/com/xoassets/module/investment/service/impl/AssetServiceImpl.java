package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.module.investment.dto.AssetRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.vo.AssetVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.mapper.AssetMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 公共资产服务实现。
 */
@Service
public class AssetServiceImpl implements AssetService {

    private static final List<String> ASSET_TYPES = List.of("STOCK", "FUND", "CRYPTO", "OTHER");
    private static final List<String> QUOTE_SOURCES = List.of("MANUAL", "COINGECKO", "EASTMONEY", "SINA", "YAHOO", "ALPHA_VANTAGE", "TUSHARE", "AKSHARE");

    private final AssetMapper assetMapper;

    public AssetServiceImpl(AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
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
     * 创建公共资产，按类型和代码防止重复。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetVO create(AssetRequest request) {
        ensureAssetType(request.getType());
        ensureQuoteSource(request.getQuoteSource());
        Long exists = assetMapper.selectCount(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getType, request.getType())
                .eq(Asset::getSymbol, request.getSymbol()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "资产已存在");
        }
        Asset asset = new Asset();
        asset.setSymbol(request.getSymbol());
        asset.setName(request.getName());
        asset.setType(request.getType());
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
     * 转换资产展示对象。
     */
    private AssetVO toVO(Asset asset) {
        return AssetVO.builder()
                .id(asset.getId())
                .symbol(asset.getSymbol())
                .name(asset.getName())
                .type(asset.getType())
                .currency(asset.getCurrency())
                .quoteSource(asset.getQuoteSource())
                .quoteKey(asset.getQuoteKey())
                .build();
    }
}
