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

    private BigDecimal totalAsset;
    private BigDecimal lastMonthChangeAmount;
    private BigDecimal lastMonthChangeRate;
    private Boolean compareAvailable;
    private Integer accountCount;
    private BigDecimal nonCreditAssetTotal;
    private Integer nonZeroAccountCount;
    private List<AccountCategorySummaryVO> categories;
    private List<AccountDisplayVO> accounts;
}
