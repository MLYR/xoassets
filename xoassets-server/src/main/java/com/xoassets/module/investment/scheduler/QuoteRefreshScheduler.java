package com.xoassets.module.investment.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * 持仓资产行情定时刷新任务；单个资产失败不会影响应用启动和下一轮任务。
 */
@Slf4j
@Component
public class QuoteRefreshScheduler {

    /**
     * 基金资产类型常量。
     */
    private static final String ASSET_TYPE_FUND = "FUND";
    /**
     * 股票资产类型常量。
     */
    private static final String ASSET_TYPE_STOCK = "STOCK";
    /**
     * 虚拟货币资产类型常量。
     */
    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";

    /**
     * 持仓数据访问组件。
     */
    private final HoldingMapper holdingMapper;
    /**
     * 资产数据访问组件。
     */
    private final AssetMapper assetMapper;
    /**
     * 行情服务。
     */
    private final QuoteService quoteService;

    /**
     * 注入定时任务依赖。
     */
    public QuoteRefreshScheduler(HoldingMapper holdingMapper, AssetMapper assetMapper, QuoteService quoteService) {
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.quoteService = quoteService;
    }

    /**
     * 股票开盘日 09:30-15:00 每 15 分钟刷新；交易日和交易窗口由 QuoteService 再兜底。
     */
    @XxlJob("refreshStockQuotes")
    public void refreshStockQuotes() {
        try {
            for (Long assetId : activeHoldingAssetIds(List.of(ASSET_TYPE_STOCK), false)) {
                refreshOne(assetId);
            }
        } catch (Exception exception) {
            log.warn("股票行情定时刷新任务执行失败", exception);
        }
    }

    /**
     * 虚拟货币 24 小时每 15 分钟刷新，和股票任务分开避免休市窗口影响。
     */
    @XxlJob("refreshCryptoQuotes")
    public void refreshCryptoQuotes() {
        try {
            for (Long assetId : activeHoldingAssetIds(List.of(ASSET_TYPE_CRYPTO), false)) {
                refreshOne(assetId);
            }
        } catch (Exception exception) {
            log.warn("虚拟货币行情定时刷新任务执行失败", exception);
        }
    }

    /**
     * 18:00 首轮强制尝试刷新基金净值，避免白天旧净值被 1 天 TTL 拦住。
     */
    @XxlJob("refreshFundQuotes")
    public void refreshFundQuotes() {
        refreshFundQuotesSafely();
    }

    /**
     * 18:15 到 23:45 每 15 分钟继续刷新，和 18:00 首轮组成完整晚间窗口。
     */
    @XxlJob("refreshFundQuotesFollowup")
    public void refreshFundQuotesFollowup() {
        refreshFundQuotesSafely();
    }

    /**
     * 安全刷新基金净值。
     */
    private void refreshFundQuotesSafely() {
        try {
            for (Long assetId : activeHoldingAssetIds(List.of(ASSET_TYPE_FUND), true)) {
                refreshOneForced(assetId);
            }
        } catch (Exception exception) {
            log.warn("基金净值晚间刷新任务执行失败", exception);
        }
    }

    /**
     * 单个资产失败只记录日志，避免中断其他资产刷新。
     */
    private void refreshOne(Long assetId) {
        try {
            quoteService.refreshQuoteIfStale(assetId);
        } catch (Exception exception) {
            log.warn("资产行情刷新失败 assetId={}", assetId, exception);
        }
    }

    /**
     * 基金晚间刷新需要绕过 TTL，因为同一自然日内第三方可能稍晚才给出新净值。
     */
    private void refreshOneForced(Long assetId) {
        try {
            quoteService.refreshQuote(assetId);
        } catch (Exception exception) {
            log.warn("资产行情强制刷新失败 assetId={}", assetId, exception);
        }
    }

    /**
     * 查询需要刷新行情的持仓资产ID。
     */
    private Set<Long> activeHoldingAssetIds(List<String> assetTypes, boolean includeZeroQuantity) {
        LambdaQueryWrapper<Holding> holdingWrapper = new LambdaQueryWrapper<Holding>()
                .select(Holding::getAssetId)
                .eq(Holding::getStatus, 1);
        if (!includeZeroQuantity) {
            holdingWrapper.gt(Holding::getQuantity, 0);
        }
        Set<Long> holdingAssetIds = holdingMapper.selectList(holdingWrapper)
                .stream()
                .map(Holding::getAssetId)
                .collect(Collectors.toSet());
        if (holdingAssetIds.isEmpty()) {
            return Set.of();
        }
        return assetMapper.selectList(new LambdaQueryWrapper<Asset>()
                        .select(Asset::getId)
                        .in(Asset::getId, holdingAssetIds)
                        .in(Asset::getType, assetTypes)
                        .eq(Asset::getStatus, 1))
                .stream()
                .map(Asset::getId)
                .collect(Collectors.toSet());
    }
}
