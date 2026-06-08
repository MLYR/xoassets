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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 持仓资产行情定时刷新任务；单个资产失败不会影响应用启动和下一轮任务。
 */
@Slf4j
@Component
public class QuoteRefreshScheduler {

    private static final String ASSET_TYPE_FUND = "FUND";
    private static final String ASSET_TYPE_STOCK = "STOCK";
    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";

    private final HoldingMapper holdingMapper;
    private final AssetMapper assetMapper;
    private final QuoteService quoteService;

    public QuoteRefreshScheduler(HoldingMapper holdingMapper, AssetMapper assetMapper, QuoteService quoteService) {
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.quoteService = quoteService;
    }

    /**
     * 股票和虚拟货币每 15 分钟刷新一次；股票交易窗口由 QuoteService 二次兜底。
     */
    @Scheduled(
            fixedDelayString = "${xoassets.quotes.refresh-fixed-delay-ms:900000}",
            initialDelayString = "${xoassets.quotes.refresh-initial-delay-ms:60000}")
    public void refreshMarketQuotes() {
        try {
            for (Long assetId : activeHoldingAssetIds(List.of(ASSET_TYPE_STOCK, ASSET_TYPE_CRYPTO), false)) {
                refreshOne(assetId);
            }
        } catch (Exception exception) {
            log.warn("股票和虚拟货币行情定时刷新任务执行失败", exception);
        }
    }

    /**
     * 基金净值晚间多次强制尝试刷新，避免白天旧净值被 1 天 TTL 拦住。
     */
    @Scheduled(cron = "${xoassets.quotes.fund-refresh-cron:0 30 19 * * ?}")
    public void refreshFundQuotes() {
        refreshFundQuotesSafely();
    }

    /**
     * 20:00 到 23:30 每半小时继续刷新，和 19:30 首轮组成完整晚间窗口。
     */
    @Scheduled(cron = "${xoassets.quotes.fund-refresh-followup-cron:0 0,30 20-23 * * ?}")
    public void refreshFundQuotesFollowup() {
        refreshFundQuotesSafely();
    }

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
