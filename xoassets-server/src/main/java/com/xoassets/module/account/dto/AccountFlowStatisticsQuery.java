package com.xoassets.module.account.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 账户详情统计查询参数，month 优先用于移动端月度视图。
 */
@Data
public class AccountFlowStatisticsQuery {

    /**
     * 月份。
     */
    private String month;
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
}
