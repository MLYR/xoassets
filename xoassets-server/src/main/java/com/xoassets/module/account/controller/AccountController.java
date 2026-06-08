package com.xoassets.module.account.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.account.dto.AccountBalanceAdjustmentRequest;
import com.xoassets.module.account.dto.AccountFlowStatisticsQuery;
import com.xoassets.module.account.dto.AccountLedgerQuery;
import com.xoassets.module.account.dto.AccountRequest;
import com.xoassets.module.account.service.AccountBalanceService;
import com.xoassets.module.account.service.AccountLedgerService;
import com.xoassets.module.account.service.AccountOverviewService;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.account.vo.AccountBalanceAdjustmentVO;
import com.xoassets.module.account.vo.AccountBalanceTrendPointVO;
import com.xoassets.module.account.vo.AccountFlowStatisticsVO;
import com.xoassets.module.account.vo.AccountLedgerPageVO;
import com.xoassets.module.account.vo.AccountOverviewVO;
import com.xoassets.module.account.vo.AccountVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户管理接口。
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;
    private final AccountLedgerService accountLedgerService;
    private final AccountOverviewService accountOverviewService;

    public AccountController(AccountService accountService, AccountBalanceService accountBalanceService, AccountLedgerService accountLedgerService, AccountOverviewService accountOverviewService) {
        this.accountService = accountService;
        this.accountBalanceService = accountBalanceService;
        this.accountLedgerService = accountLedgerService;
        this.accountOverviewService = accountOverviewService;
    }

    /**
     * 查询当前用户账户列表。
     */
    @GetMapping
    public Result<List<AccountVO>> list() {
        return Result.success(accountService.list());
    }

    /**
     * 查询移动端账户页聚合数据。
     */
    @GetMapping("/overview")
    public Result<AccountOverviewVO> overview() {
        return Result.success(accountOverviewService.overview());
    }

    /**
     * 查询账户资金明细，聚合普通流水和投资交易。
     */
    @GetMapping("/{id}/ledger")
    public Result<AccountLedgerPageVO> ledger(@PathVariable Long id, AccountLedgerQuery query) {
        return Result.success(accountLedgerService.ledger(id, query));
    }

    /**
     * 查询账户详情统计。
     */
    @GetMapping("/{id}/flow-statistics")
    public Result<AccountFlowStatisticsVO> flowStatistics(@PathVariable Long id, AccountFlowStatisticsQuery query) {
        return Result.success(accountLedgerService.flowStatistics(id, query));
    }

    /**
     * 查询账户日终余额曲线。
     */
    @GetMapping("/{id}/balance-trend")
    public Result<List<AccountBalanceTrendPointVO>> balanceTrend(@PathVariable Long id, LocalDate startDate, LocalDate endDate) {
        return Result.success(accountBalanceService.balanceTrend(id, startDate, endDate));
    }

    /**
     * 账户余额修正：生成专用调整事件，不计入普通收支。
     */
    @PostMapping("/{id}/balance-adjustments")
    public Result<AccountBalanceAdjustmentVO> adjustBalance(@PathVariable Long id, @Valid @RequestBody AccountBalanceAdjustmentRequest request) {
        return Result.success(accountBalanceService.adjustBalance(id, request));
    }

    /**
     * 创建账户。
     */
    @PostMapping
    public Result<AccountVO> create(@Valid @RequestBody AccountRequest request) {
        return Result.success(accountService.create(request));
    }

    /**
     * 更新账户基础信息。
     */
    @PutMapping("/{id}")
    public Result<AccountVO> update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return Result.success(accountService.update(id, request));
    }

    /**
     * 删除未被流水使用的账户。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Result.success(null);
    }
}
