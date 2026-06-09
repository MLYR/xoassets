package com.xoassets.module.account.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 账户资金明细查询参数，type 使用统一后的 bizType。
 */
@Data
public class AccountLedgerQuery {

    /**
     * 页码。
     */
    private long pageNo = 1;
    /**
     * 每页条数。
     */
    private long pageSize = 10;
    /**
     * 业务类型。
     */
    private String type;
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
     * 搜索关键词。
     */
    private String keyword;
}
