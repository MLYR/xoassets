package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 账户页分类汇总，用于移动端账户总览的分类卡片和分布条。
 */
@Data
@Builder
public class AccountCategorySummaryVO {

    /**
     * 账户分组。
     */
    private String group;
    /**
     * 展示标签。
     */
    private String label;
    /**
     * 金额。
     */
    private BigDecimal amount;
    /**
     * 占比。
     */
    private BigDecimal ratio;
    /**
     * 数量。
     */
    private Integer count;
    /**
     * 颜色键。
     */
    private String colorKey;
}
