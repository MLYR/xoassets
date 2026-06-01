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

    private AccountVO account;
    private AccountLedgerSummaryVO summary;
    private PageResult<AccountLedgerVO> page;
}
