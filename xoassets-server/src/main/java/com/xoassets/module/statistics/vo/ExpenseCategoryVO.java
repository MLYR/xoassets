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

    /**
     * 分类ID。
     */
    private Long categoryId;
    /**
     * 分类名称。
     */
    private String categoryName;
    /**
     * 金额。
     */
    private BigDecimal amount;
    /**
     * 占比。
     */
    private BigDecimal percent;
}
