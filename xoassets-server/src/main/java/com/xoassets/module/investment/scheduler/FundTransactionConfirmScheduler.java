package com.xoassets.module.investment.scheduler;

import com.xoassets.module.investment.service.InvestmentTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 基金金额买入确认任务，负责把待确认交易按已入库单位净值转换为确认份额。
 */
@Slf4j
@Component
public class FundTransactionConfirmScheduler {

    private final InvestmentTransactionService transactionService;

    public FundTransactionConfirmScheduler(InvestmentTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 定时扫描待确认基金交易；服务层用状态条件更新保证幂等，避免重复累加持仓。
     */
    @Scheduled(
            fixedDelayString = "${xoassets.fund-confirm.fixed-delay-ms:1800000}",
            initialDelayString = "${xoassets.fund-confirm.initial-delay-ms:120000}")
    public void confirmPendingFundTransactions() {
        try {
            transactionService.confirmPendingFundBuys();
        } catch (Exception exception) {
            log.warn("基金待确认交易扫描失败", exception);
        }
    }
}
