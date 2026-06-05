package com.xoassets.module.account.service;

import com.xoassets.module.account.dto.AccountBalanceAdjustmentRequest;
import com.xoassets.module.account.vo.AccountBalanceAdjustmentVO;
import com.xoassets.module.account.vo.AccountBalanceTrendPointVO;
import java.time.LocalDate;
import java.util.List;

/**
 * 账户余额修正和日终余额曲线服务。
 */
public interface AccountBalanceService {

    /**
     * 按修正后余额生成审计事件，并原子更新账户余额。
     */
    AccountBalanceAdjustmentVO adjustBalance(Long accountId, AccountBalanceAdjustmentRequest request);

    /**
     * 查询账户日终余额曲线。
     */
    List<AccountBalanceTrendPointVO> balanceTrend(Long accountId, LocalDate startDate, LocalDate endDate);
}
