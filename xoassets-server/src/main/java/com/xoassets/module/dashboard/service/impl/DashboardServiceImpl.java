package com.xoassets.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.budget.service.BudgetService;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.vo.InvestmentOverviewVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 首页服务：提供 MVP 仪表盘所需的汇总指标。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final AccountMapper accountMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final HoldingService holdingService;
    private final BudgetService budgetService;

    public DashboardServiceImpl(
            AccountMapper accountMapper,
            TransactionRecordMapper transactionRecordMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            HoldingService holdingService,
            BudgetService budgetService) {
        this.accountMapper = accountMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.holdingService = holdingService;
        this.budgetService = budgetService;
    }

    /**
     * 计算首页概览指标；趋势率用当前月与上月同口径数据对比。
     */
    @Override
    public DashboardOverviewVO overview(YearMonth month) {
        Long userId = LoginUserContext.getUserId();
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        YearMonth previousMonth = targetMonth.minusMonths(1);

        AccountAssetSummary accountSummary = accountAssetSummary(userId);
        InvestmentOverviewVO investmentOverview = holdingService.overview();
        // 首页投资指标直接复用投资模块总览，确保“今日盈亏”和投资页今日收益同源同口径。
        BigDecimal investmentMarketValue = nullToZero(investmentOverview.getTotalInvestmentAsset());
        BigDecimal investmentFloatingProfit = nullToZero(investmentOverview.getHoldingProfit());
        // 首页“投资盈亏(总)”展示投资总收益：已实现卖出收益 + 当前持仓浮动收益。
        BigDecimal investmentTotalProfit = investmentFloatingProfit.add(realizedInvestmentProfit(userId, LocalDate.now()));
        BigDecimal investmentYesterdayProfit = investmentOverview.getYesterdayProfit();
        BigDecimal investmentTodayProfit = investmentOverview.getTodayProfit();
        BigDecimal totalAssets = accountSummary.cashAsset().add(investmentMarketValue);
        BigDecimal netAssets = totalAssets.subtract(accountSummary.liability());
        BigDecimal budgetUsageRate = safeBudgetUsageRate(targetMonth);

        BigDecimal monthlyIncome = sumIncome(userId, targetMonth);
        BigDecimal monthlyExpense = sumExpense(userId, targetMonth);
        BigDecimal todayIncome = sumIncome(userId, LocalDate.now());
        BigDecimal todayExpense = sumExpense(userId, LocalDate.now());
        BigDecimal yesterdayIncome = sumIncome(userId, LocalDate.now().minusDays(1));
        BigDecimal yesterdayExpense = sumExpense(userId, LocalDate.now().minusDays(1));
        BigDecimal todayBalance = todayIncome.subtract(todayExpense);
        BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense);

        BigDecimal previousIncome = sumIncome(userId, previousMonth);
        BigDecimal previousExpense = sumExpense(userId, previousMonth);
        BigDecimal previousBalance = previousIncome.subtract(previousExpense);

        return DashboardOverviewVO.builder()
                .totalAssets(totalAssets)
                .netAssets(netAssets)
                .todayIncome(todayIncome)
                .todayExpense(todayExpense)
                .yesterdayIncome(yesterdayIncome)
                .yesterdayExpense(yesterdayExpense)
                .monthlyIncome(monthlyIncome)
                .monthlyExpense(monthlyExpense)
                .todayBalance(todayBalance)
                .monthlyBalance(monthlyBalance)
                .todayBalanceRateByIncome(balanceRate(todayBalance, todayIncome))
                .todayBalanceRateByExpense(balanceRate(todayBalance, todayExpense))
                .monthlyBalanceRateByIncome(balanceRate(monthlyBalance, monthlyIncome))
                .monthlyBalanceRateByExpense(balanceRate(monthlyBalance, monthlyExpense))
                .investmentMarketValue(investmentMarketValue)
                .investmentFloatingProfit(investmentFloatingProfit)
                .investmentTotalProfit(investmentTotalProfit)
                .investmentYesterdayProfit(investmentYesterdayProfit)
                .investmentTodayProfit(investmentTodayProfit)
                .budgetUsageRate(budgetUsageRate)
                .assetTrendRate(null)
                .incomeTrendRate(rate(monthlyIncome, previousIncome))
                .expenseTrendRate(rate(monthlyExpense, previousExpense))
                .balanceTrendRate(rate(monthlyBalance, previousBalance))
                .build();
    }

    /**
     * 统计指定用户某月收入总额。
     */
    @Override
    public BigDecimal sumIncome(Long userId, YearMonth month) {
        return transactionRecordMapper.selectList(monthWrapper(userId, month)
                        .eq(TransactionRecord::getType, "INCOME"))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 统计指定用户某月支出总额，退款会抵扣支出且结果不低于 0。
     */
    @Override
    public BigDecimal sumExpense(Long userId, YearMonth month) {
        BigDecimal expense = transactionRecordMapper.selectList(monthWrapper(userId, month)
                        .eq(TransactionRecord::getType, "EXPENSE"))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = transactionRecordMapper.selectList(monthWrapper(userId, month)
                        .eq(TransactionRecord::getType, "REFUND"))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return expense.subtract(refund).max(BigDecimal.ZERO);
    }

    /**
     * 统计指定用户某日支出，退款按同日抵扣支出。
     */
    private BigDecimal sumExpense(Long userId, LocalDate date) {
        BigDecimal expense = transactionRecordMapper.selectList(dayWrapper(userId, date)
                        .eq(TransactionRecord::getType, "EXPENSE"))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = transactionRecordMapper.selectList(dayWrapper(userId, date)
                        .eq(TransactionRecord::getType, "REFUND"))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return expense.subtract(refund).max(BigDecimal.ZERO);
    }

    /**
     * 统计指定用户某日收入。
     */
    private BigDecimal sumIncome(Long userId, LocalDate date) {
        return transactionRecordMapper.selectList(dayWrapper(userId, date)
                        .eq(TransactionRecord::getType, "INCOME"))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 统计截至指定日期已实现投资收益，只取正常/已确认卖出，撤销交易不参与首页总收益。
     */
    private BigDecimal realizedInvestmentProfit(Long userId, LocalDate date) {
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .eq(InvestmentTransaction::getType, "SELL")
                        .in(InvestmentTransaction::getStatus, "NORMAL", "CONFIRMED")
                        .le(InvestmentTransaction::getTransactionTime, end))
                .stream()
                .map(InvestmentTransaction::getRealizedProfit)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 读取预算使用率；预算尚未创建时返回 0，避免首页展示中断。
     */
    private BigDecimal safeBudgetUsageRate(YearMonth month) {
        BudgetSummaryVO summary = budgetService.summary(month.toString());
        return summary.getUsageRate() == null ? BigDecimal.ZERO : summary.getUsageRate();
    }

    /**
     * 账户余额按财务口径拆分：正余额是现金资产，负余额绝对值是负债。
     */
    private AccountAssetSummary accountAssetSummary(Long userId) {
        BigDecimal cashAsset = BigDecimal.ZERO;
        BigDecimal liability = BigDecimal.ZERO;
        List<Account> accounts = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId)
                .eq(Account::getStatus, 1));
        for (Account account : accounts) {
            BigDecimal balance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                cashAsset = cashAsset.add(balance);
            } else {
                liability = liability.add(balance.abs());
            }
        }
        return new AccountAssetSummary(cashAsset, liability);
    }

    /**
     * 构造月份时间范围查询条件，包含整月首日到末日。
     */
    private LambdaQueryWrapper<TransactionRecord> monthWrapper(Long userId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .between(TransactionRecord::getTransactionTime, LocalDateTime.of(startDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX));
    }

    /**
     * 构造指定日期的流水查询条件。
     */
    private LambdaQueryWrapper<TransactionRecord> dayWrapper(Long userId, LocalDate date) {
        return new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .between(TransactionRecord::getTransactionTime, LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
    }

    /**
     * 计算环比百分比；上期为 0 时返回 null，让前端展示 --，避免把缺失基准冒充成 0。
     */
    private BigDecimal rate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 4, RoundingMode.HALF_UP);
    }

    /**
     * 盈亏率支持收入和支出两个分母；分母为 0 时返回 null，让前端展示 --。
     */
    private BigDecimal balanceRate(BigDecimal balance, BigDecimal denominator) {
        if (balance == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return balance.multiply(BigDecimal.valueOf(100)).divide(denominator.abs(), 4, RoundingMode.HALF_UP);
    }

    /**
     * 投资模块部分字段允许为 null，首页资产总额计算时统一按 0 兜底。
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 首页资产拆分结果。
     */
    private record AccountAssetSummary(BigDecimal cashAsset, BigDecimal liability) {
    }
}
