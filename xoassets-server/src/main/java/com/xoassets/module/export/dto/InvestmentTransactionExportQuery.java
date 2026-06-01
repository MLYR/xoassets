package com.xoassets.module.export.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 投资交易导出参数，所有条件都按当前 user_id 过滤。
 */
@Data
public class InvestmentTransactionExportQuery {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private Long holdingId;
    private Long assetId;
    private Long accountId;
}
