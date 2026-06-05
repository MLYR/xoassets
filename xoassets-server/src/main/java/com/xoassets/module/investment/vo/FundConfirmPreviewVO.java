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

    private LocalDate tradeDate;
    private LocalDate effectiveTradeDate;
    private LocalDate confirmedDate;
    private Boolean qdii;
    private Boolean shifted;
    private String shiftReason;
}
