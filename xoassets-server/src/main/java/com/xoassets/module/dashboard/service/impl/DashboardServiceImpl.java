package com.xoassets.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.budget.service.BudgetService;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.InvestmentTransactionService;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.transaction.dto.TransactionQuery;
import com.xoassets.module.transaction.service.TransactionService;
import com.xoassets.module.transaction.vo.TransactionVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
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
 * 首页服务：提供 MVP 仪表盘所需的汇总指标和最近流水。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final AccountMapper accountMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final TransactionService transactionService;
    private final HoldingService holdingService;
    private final InvestmentTransactionService investmentTransactionService;
    private final BudgetService budgetService;

    public DashboardServiceImpl(
            AccountMapper accountMapper,
            TransactionRecordMapper transactionRecordMapper,
            TransactionService transactionService,
            HoldingService holdingService,
            InvestmentTransactionService investmentTransactionService,
            BudgetService budgetService) {
        this.accountMapper = accountMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.transactionService = transactionService;
        this.holdingService = holdingService;
        this.investmentTransactionService = investmentTransactionService;
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

        BigDecimal accountAssets = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
                        .eq(Account::getStatus, 1))
                .stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<HoldingVO> holdings = holdingService.list();
        BigDecimal investmentMarketValue = holdings.stream().map(HoldingVO::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investmentFloatingProfit = holdings.stream().map(HoldingVO::getFloatingProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAssets = accountAssets.add(investmentMarketValue);
        BigDecimal budgetUsageRate = safeBudgetUsageRate(targetMonth);

        BigDecimal monthlyIncome = sumIncome(userId, targetMonth);
        BigDecimal monthlyExpense = sumExpense(userId, targetMonth);
        BigDecimal todayExpense = sumExpense(userId, LocalDate.now());
        BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense);

        BigDecimal previousIncome = sumIncome(userId, previousMonth);
        BigDecimal previousExpense = sumExpense(userId, previousMonth);
        BigDecimal previousBalance = previousIncome.subtract(previousExpense);

        return DashboardOverviewVO.builder()
                .totalAssets(totalAssets)
                .netAssets(totalAssets)
                .todayExpense(todayExpense)
                .monthlyIncome(monthlyIncome)
                .monthlyExpense(monthlyExpense)
                .monthlyBalance(monthlyBalance)
                .investmentMarketValue(investmentMarketValue)
                .investmentFloatingProfit(investmentFloatingProfit)
                .budgetUsageRate(budgetUsageRate)
                .assetTrendRate(BigDecimal.ZERO)
                .incomeTrendRate(rate(monthlyIncome, previousIncome))
                .expenseTrendRate(rate(monthlyExpense, previousExpense))
                .balanceTrendRate(rate(monthlyBalance, previousBalance))
                .recentTransactions(recentTransactions(5))
                .recentInvestmentTransactions(investmentTransactionService.list(null).stream().limit(5).toList())
                .build();
    }

    /**
     * 查询最近流水，限制 limit 防止首页一次性拉取过多数据。
     */
    @Override
    public List<TransactionVO> recentTransactions(int limit) {
        TransactionQuery query = new TransactionQuery();
        query.setPageNo(1);
        query.setPageSize(Math.max(1, Math.min(limit, 20)));
        return transactionService.page(query).getRecords();
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
     * 读取预算使用率；预算尚未创建时返回 0，避免首页展示中断。
     */
    private BigDecimal safeBudgetUsageRate(YearMonth month) {
        BudgetSummaryVO summary = budgetService.summary(month.toString());
        return summary.getUsageRate() == null ? BigDecimal.ZERO : summary.getUsageRate();
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
     * 计算环比百分比；上期为 0 时返回 0，避免除零和误导性无穷大。
     */
    private BigDecimal rate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 4, RoundingMode.HALF_UP);
    }
}
