package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 移动端账户页聚合数据，避免前端为原型 UI 拼装业务口径。
 */
@Data
@Builder
public class AccountOverviewVO {

    /**
     * 总资产。
     */
    private BigDecimal totalAsset;
    /**
     * 较上月变化金额。
     */
    private BigDecimal lastMonthChangeAmount;
    /**
     * 较上月变化率。
     */
    private BigDecimal lastMonthChangeRate;
    /**
     * 是否可进行对比。
     */
    private Boolean compareAvailable;
    /**
     * 账户数量。
     */
    private Integer accountCount;
    /**
     * 非信用账户资产合计。
     */
    private BigDecimal nonCreditAssetTotal;
    /**
     * 非零余额账户数。
     */
    private Integer nonZeroAccountCount;
    /**
     * 分类列表。
     */
    private List<AccountCategorySummaryVO> categories;
    /**
     * 账户列表。
     */
    private List<AccountDisplayVO> accounts;
}
