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

    private String group;
    private String label;
    private BigDecimal amount;
    private BigDecimal ratio;
    private Integer count;
    private String colorKey;
}
