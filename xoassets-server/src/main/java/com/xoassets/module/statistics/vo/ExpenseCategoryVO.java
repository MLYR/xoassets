package com.xoassets.module.statistics.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 支出分类占比。
 */
@Data
@Builder
public class ExpenseCategoryVO {

    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private BigDecimal percent;
}
