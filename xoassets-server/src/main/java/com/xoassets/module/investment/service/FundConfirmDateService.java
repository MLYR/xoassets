package com.xoassets.module.investment.service;

import com.xoassets.module.investment.vo.FundConfirmPreviewVO;
import com.xoassets.persistence.entity.Asset;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金金额买入确认日期计算服务。
 */
public interface FundConfirmDateService {

    /**
     * 计算基金申购的有效申请日，15:00 后或非交易日顺延。
     */
    LocalDate effectiveTradeDate(Asset asset, LocalDateTime transactionTime);

    /**
     * 计算基金申购预计确认日，普通基金 T+1，QDII T+2。
     */
    LocalDate confirmedDate(Asset asset, LocalDateTime transactionTime);

    /**
     * 根据实际买入时间和基金类型预估确认日期。
     */
    FundConfirmPreviewVO preview(Asset asset, LocalDateTime transactionTime);

    /**
     * 判断指定日期是否为当前规则下的基金交易日。
     */
    boolean isTradingDay(LocalDate date);
}
