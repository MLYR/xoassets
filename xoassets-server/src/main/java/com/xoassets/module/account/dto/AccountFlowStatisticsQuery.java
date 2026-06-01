package com.xoassets.module.account.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 账户资金流向统计查询参数，month 优先用于月度视图。
 */
@Data
public class AccountFlowStatisticsQuery {

    private String month;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
