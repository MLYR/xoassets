package com.xoassets.module.transaction.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 流水分页查询参数。
 */
@Data
public class TransactionQuery {

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
     * 账户ID。
     */
    private Long accountId;
    /**
     * 分类ID。
     */
    private Long categoryId;
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
