package com.xoassets.module.export.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 投资交易导出参数，所有条件都按当前 user_id 过滤。
 */
@Data
public class InvestmentTransactionExportQuery {

    /**
     * 开始日期。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    /**
     * 结束日期。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    /**
     * 持仓ID。
     */
    private Long holdingId;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 账户ID。
     */
    private Long accountId;
}
