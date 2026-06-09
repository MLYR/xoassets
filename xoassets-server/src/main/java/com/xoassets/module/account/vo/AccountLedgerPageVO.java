package com.xoassets.module.account.vo;

import com.xoassets.common.api.PageResult;
import lombok.Builder;
import lombok.Data;

/**
 * 账户详情资金明细返回结构，包含账户基础信息、汇总和分页明细。
 */
@Data
@Builder
public class AccountLedgerPageVO {

    /**
     * 账户信息。
     */
    private AccountVO account;
    /**
     * 摘要。
     */
    private AccountLedgerSummaryVO summary;
    /**
     * 分页数据。
     */
    private PageResult<AccountLedgerVO> page;
}
