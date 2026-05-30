package com.xoassets.module.transaction.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 流水分页查询参数。
 */
@Data
public class TransactionQuery {

    private long pageNo = 1;
    private long pageSize = 10;
    private String type;
    private Long accountId;
    private Long categoryId;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String keyword;
}
