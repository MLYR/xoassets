package com.xoassets.module.investment.vo;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * 基金金额买入确认日期预估结果。
 */
@Data
@Builder
public class FundConfirmPreviewVO {

    /**
     * 交易日期。
     */
    private LocalDate tradeDate;
    /**
     * 有效申请日。
     */
    private LocalDate effectiveTradeDate;
    /**
     * 确认日期。
     */
    private LocalDate confirmedDate;
    /**
     * 是否QDII基金。
     */
    private Boolean qdii;
    /**
     * 是否发生顺延。
     */
    private Boolean shifted;
    /**
     * 顺延原因。
     */
    private String shiftReason;
}
