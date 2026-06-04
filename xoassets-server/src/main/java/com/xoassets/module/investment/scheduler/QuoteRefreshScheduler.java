package com.xoassets.module.investment.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.mapper.HoldingMapper;
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

    private final HoldingMapper holdingMapper;
    private final QuoteService quoteService;

    public QuoteRefreshScheduler(HoldingMapper holdingMapper, QuoteService quoteService) {
        this.holdingMapper = holdingMapper;
        this.quoteService = quoteService;
    }

    /**
     * 定时刷新所有用户持仓涉及到的资产，QuoteService 内部负责新鲜度判断。
     */
    @Scheduled(
            fixedDelayString = "${xoassets.quotes.refresh-fixed-delay-ms:300000}",
            initialDelayString = "${xoassets.quotes.refresh-initial-delay-ms:60000}")
    public void refreshHoldingQuotes() {
        try {
            Set<Long> assetIds = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                            .select(Holding::getAssetId)
                            .eq(Holding::getStatus, 1)
                            .gt(Holding::getQuantity, 0))
                    .stream()
                    .map(Holding::getAssetId)
                    .collect(Collectors.toSet());
            for (Long assetId : assetIds) {
                refreshOne(assetId);
            }
        } catch (Exception exception) {
            log.warn("持仓行情定时刷新任务执行失败", exception);
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
}
