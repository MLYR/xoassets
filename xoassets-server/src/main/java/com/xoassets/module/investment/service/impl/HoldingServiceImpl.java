package com.xoassets.module.investment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.investment.dto.HoldingRequest;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户持仓服务实现。
 */
@Service
public class HoldingServiceImpl implements HoldingService {

    private static final List<String> ASSET_TYPES = List.of("STOCK", "FUND", "CRYPTO", "OTHER");
    private static final List<String> QUOTE_SOURCES = List.of("MANUAL", "COINGECKO", "ALPHA_VANTAGE", "TUSHARE", "AKSHARE");

    private final HoldingMapper holdingMapper;
    private final AssetMapper assetMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final AssetService assetService;
    private final QuoteService quoteService;

    public HoldingServiceImpl(
            HoldingMapper holdingMapper,
            AssetMapper assetMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            AssetService assetService,
            QuoteService quoteService) {
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.assetService = assetService;
        this.quoteService = quoteService;
    }

    /**
     * 查询当前用户持仓并补齐价格估值。
     */
    @Override
    public java.util.List<HoldingVO> list() {
        Long userId = LoginUserContext.getUserId();
        java.util.List<Holding> holdings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .orderByDesc(Holding::getCreatedAt));
        return toVOList(holdings);
    }

    /**
     * 手动新增持仓，总成本按数量乘以平均成本初始化。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public HoldingVO create(HoldingRequest request) {
        Long userId = LoginUserContext.getUserId();
        Asset asset = resolveAsset(request);
        BigDecimal quantity = scale4(request.getQuantity());
        BigDecimal avgCost = scale4(request.getAvgCost());
        ensureNoDuplicatedHolding(userId, asset.getId(), null);
        Holding holding = new Holding();
        holding.setUserId(userId);
        holding.setAssetId(asset.getId());
        holding.setQuantity(quantity);
        holding.setAvgCost(avgCost);
        holding.setTotalCost(quantity.multiply(avgCost).setScale(4, RoundingMode.HALF_UP));
        holding.setRemark(request.getRemark());
        holding.setStatus(1);
        holding.setDeleted(0);
        holdingMapper.insert(holding);
        return toVO(holding);
    }

    /**
     * 修改当前用户自己的持仓基础信息。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public HoldingVO update(Long id, HoldingRequest request) {
        Long userId = LoginUserContext.getUserId();
        Asset asset = resolveAsset(request);
        Holding holding = findOwnedHolding(id, userId);
        BigDecimal quantity = scale4(request.getQuantity());
        BigDecimal avgCost = scale4(request.getAvgCost());
        ensureNoDuplicatedHolding(userId, asset.getId(), id);
        holding.setAssetId(asset.getId());
        holding.setQuantity(quantity);
        holding.setAvgCost(avgCost);
        holding.setTotalCost(quantity.multiply(avgCost).setScale(4, RoundingMode.HALF_UP));
        holding.setRemark(request.getRemark());
        holdingMapper.update(holding, new LambdaUpdateWrapper<Holding>()
                .eq(Holding::getId, id)
                .eq(Holding::getUserId, userId));
        return toVO(holding);
    }

    /**
     * 删除持仓前检查是否已有投资交易，避免历史交易失去持仓归属。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        findOwnedHolding(id, userId);
        Long transactionCount = investmentTransactionMapper.selectCount(new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getHoldingId, id));
        if (transactionCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓已有投资交易，第一版不允许删除");
        }
        holdingMapper.delete(new LambdaQueryWrapper<Holding>().eq(Holding::getId, id).eq(Holding::getUserId, userId));
    }

    /**
     * 查询当前用户持仓，不存在时返回业务错误。
     */
    @Override
    public Holding findOwnedHolding(Long id, Long userId) {
        Holding holding = holdingMapper.selectOne(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getId, id)
                .eq(Holding::getUserId, userId));
        if (holding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "持仓不存在");
        }
        return holding;
    }

    /**
     * 买入时使用移动平均成本法更新数量、总成本和平均成本。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Holding applyBuy(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal price, BigDecimal fee) {
        quantity = scale4(quantity);
        price = scale4(price);
        fee = scale4(fee);
        Holding holding = holdingId == null ? findOrCreateHolding(userId, assetId) : findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        BigDecimal buyCost = quantity.multiply(price).add(fee).setScale(4, RoundingMode.HALF_UP);
        BigDecimal newQuantity = holding.getQuantity().add(quantity);
        BigDecimal newTotalCost = holding.getTotalCost().add(buyCost);
        holding.setQuantity(newQuantity);
        holding.setTotalCost(newTotalCost);
        holding.setAvgCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : newTotalCost.divide(newQuantity, 4, RoundingMode.HALF_UP));
        updateHoldingBalance(holding);
        return holding;
    }

    /**
     * 卖出时校验持仓数量不能不足，并按当前平均成本减少总成本。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Holding applySell(Long userId, Long holdingId, Long assetId, BigDecimal quantity) {
        quantity = scale4(quantity);
        Holding holding = holdingId == null ? findHoldingByAsset(userId, assetId) : findOwnedHolding(holdingId, userId);
        if (!Objects.equals(holding.getAssetId(), assetId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易资产与持仓资产不一致");
        }
        if (holding.getQuantity().compareTo(quantity) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "持仓数量不足");
        }
        BigDecimal newQuantity = holding.getQuantity().subtract(quantity);
        BigDecimal newTotalCost = holding.getAvgCost().multiply(newQuantity).setScale(4, RoundingMode.HALF_UP);
        holding.setQuantity(newQuantity);
        holding.setTotalCost(newTotalCost);
        holding.setAvgCost(newQuantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : holding.getAvgCost());
        updateHoldingBalance(holding);
        return holding;
    }

    /**
     * 查询或创建用户某资产持仓，供买入交易自动建仓。
     */
    private Holding findOrCreateHolding(Long userId, Long assetId) {
        Holding holding = holdingMapper.selectOne(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getAssetId, assetId));
        if (holding != null) {
            return holding;
        }
        assetService.findAsset(assetId);
        Holding created = new Holding();
        created.setUserId(userId);
        created.setAssetId(assetId);
        created.setQuantity(BigDecimal.ZERO);
        created.setAvgCost(BigDecimal.ZERO);
        created.setTotalCost(BigDecimal.ZERO);
        created.setStatus(1);
        created.setDeleted(0);
        holdingMapper.insert(created);
        return created;
    }

    /**
     * 按资产查询当前用户持仓，卖出时必须存在。
     */
    private Holding findHoldingByAsset(Long userId, Long assetId) {
        Holding holding = holdingMapper.selectOne(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getAssetId, assetId));
        if (holding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "持仓不存在");
        }
        return holding;
    }

    /**
     * 更新持仓数量和成本，按 user_id 限定写入范围。
     */
    private void updateHoldingBalance(Holding holding) {
        holdingMapper.update(holding, new LambdaUpdateWrapper<Holding>()
                .eq(Holding::getId, holding.getId())
                .eq(Holding::getUserId, holding.getUserId()));
    }

    /**
     * 投资模块统一按四位小数参与持仓和成本计算，避免不同入口传入精度不一致。
     */
    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 同一用户同一资产只保留一条持仓。
     */
    private void ensureNoDuplicatedHolding(Long userId, Long assetId, Long excludeId) {
        LambdaQueryWrapper<Holding> wrapper = new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getAssetId, assetId);
        if (excludeId != null) {
            wrapper.ne(Holding::getId, excludeId);
        }
        if (holdingMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该资产已存在持仓");
        }
    }

    /**
     * 批量转换持仓列表。
     */
    private java.util.List<HoldingVO> toVOList(java.util.List<Holding> holdings) {
        if (holdings.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> assetIds = holdings.stream().map(Holding::getAssetId).collect(Collectors.toSet());
        Map<Long, Asset> assetMap = assetMapper.selectBatchIds(assetIds).stream().collect(Collectors.toMap(Asset::getId, asset -> asset));
        Map<Long, AssetPrice> priceMap = quoteService.latestPriceMap(assetIds);
        return holdings.stream().map(holding -> toVO(holding, assetMap.get(holding.getAssetId()), priceMap.get(holding.getAssetId()))).toList();
    }

    /**
     * 转换单条持仓。
     */
    private HoldingVO toVO(Holding holding) {
        Asset asset = assetMapper.selectById(holding.getAssetId());
        AssetPrice price = quoteService.latestPriceMap(Set.of(holding.getAssetId())).get(holding.getAssetId());
        return toVO(holding, asset, price);
    }

    /**
     * 计算市值、浮动盈亏和收益率；没有价格时用 avgCost 兜底。
     */
    private HoldingVO toVO(Holding holding, Asset asset, AssetPrice price) {
        // 只使用与资产币种一致的价格快照，避免当前价展示币种和市值计算币种不一致。
        AssetPrice matchedPrice = priceMatchesAssetCurrency(asset, price) ? price : null;
        BigDecimal latestPrice = matchedPrice == null ? holding.getAvgCost() : matchedPrice.getPrice();
        BigDecimal marketValue = holding.getQuantity().multiply(latestPrice).setScale(4, RoundingMode.HALF_UP);
        BigDecimal profit = marketValue.subtract(holding.getTotalCost());
        BigDecimal profitRate = holding.getTotalCost().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(holding.getTotalCost(), 4, RoundingMode.HALF_UP);
        return HoldingVO.builder()
                .id(holding.getId())
                .assetId(holding.getAssetId())
                .assetName(asset == null ? null : asset.getName())
                .symbol(asset == null ? null : asset.getSymbol())
                .assetType(asset == null ? null : asset.getType())
                .quoteSource(asset == null ? null : asset.getQuoteSource())
                .currency(asset == null ? null : asset.getCurrency())
                .quantity(holding.getQuantity())
                .avgCost(holding.getAvgCost())
                .totalCost(holding.getTotalCost())
                .latestPrice(latestPrice)
                .priceScale(priceScale(asset))
                .latestPriceTime(matchedPrice == null ? null : matchedPrice.getQuoteTime())
                .marketValue(marketValue)
                .floatingProfit(profit)
                .floatingProfitRate(profitRate)
                .remark(holding.getRemark())
                .status(holding.getStatus())
                .build();
    }

    /**
     * 前端只暴露“持仓”表单；这里把资产信息解析为内部公共资产，兼容原 assetId 调用。
     */
    private Asset resolveAsset(HoldingRequest request) {
        if (request.getAssetId() != null) {
            return assetService.findAsset(request.getAssetId());
        }
        ensureAssetFields(request);
        String symbol = request.getSymbol().trim().toUpperCase();
        String type = request.getAssetType();
        Asset exists = assetMapper.selectOne(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getType, type)
                .eq(Asset::getSymbol, symbol)
                .last("LIMIT 1"));
        if (exists != null) {
            return exists;
        }
        Asset asset = new Asset();
        asset.setSymbol(symbol);
        asset.setName(request.getAssetName().trim());
        asset.setType(type);
        asset.setCurrency(request.getCurrency());
        asset.setQuoteSource(request.getQuoteSource());
        asset.setQuoteKey(StringUtils.hasText(request.getQuoteKey()) ? request.getQuoteKey().trim() : symbol);
        asset.setStatus(1);
        asset.setDeleted(0);
        assetMapper.insert(asset);
        return asset;
    }

    /**
     * 虚拟货币当前价至少展示六位小数，股票和基金保留四位小数。
     */
    private Integer priceScale(Asset asset) {
        return asset != null && "CRYPTO".equals(asset.getType()) ? 6 : 4;
    }

    /**
     * 价格快照币种必须与资产币种一致，否则第一版不做跨币种估值换算。
     */
    private boolean priceMatchesAssetCurrency(Asset asset, AssetPrice price) {
        if (asset == null || price == null) {
            return false;
        }
        return Objects.equals(asset.getCurrency(), price.getCurrency());
    }

    /**
     * 校验持仓内联资产字段，资产表仍作为行情和价格快照的内部基础数据。
     */
    private void ensureAssetFields(HoldingRequest request) {
        if (!StringUtils.hasText(request.getAssetName()) || !StringUtils.hasText(request.getSymbol())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入持仓名称和资产代码");
        }
        if (!ASSET_TYPES.contains(request.getAssetType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资产类型只支持 STOCK、FUND、CRYPTO、OTHER");
        }
        if (!QUOTE_SOURCES.contains(request.getQuoteSource())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "行情来源不支持");
        }
        if (!StringUtils.hasText(request.getCurrency())) {
            request.setCurrency("CNY");
        }
    }
}
