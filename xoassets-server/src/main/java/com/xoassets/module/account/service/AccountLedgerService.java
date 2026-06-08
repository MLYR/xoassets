package com.xoassets.module.account.service;

import com.xoassets.module.account.dto.AccountFlowStatisticsQuery;
import com.xoassets.module.account.dto.AccountLedgerQuery;
import com.xoassets.module.account.vo.AccountFlowStatisticsVO;
import com.xoassets.module.account.vo.AccountLedgerPageVO;

/**
 * 账户资金中心服务，聚合普通流水和投资交易。
 */
public interface AccountLedgerService {

    /**
     * 查询账户资金明细分页。
     */
    AccountLedgerPageVO ledger(Long accountId, AccountLedgerQuery query);

    /**
     * 查询账户详情统计。
     */
    AccountFlowStatisticsVO flowStatistics(Long accountId, AccountFlowStatisticsQuery query);
}
