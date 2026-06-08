package com.xoassets.module.account.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 账户资金明细查询参数，type 使用统一后的 bizType。
 */
@Data
public class AccountLedgerQuery {

    private long pageNo = 1;
    private long pageSize = 10;
    private String type;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String keyword;
}
