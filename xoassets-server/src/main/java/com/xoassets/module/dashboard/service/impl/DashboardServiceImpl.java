package com.xoassets.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
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

    public DashboardServiceImpl(AccountMapper accountMapper, TransactionRecordMapper transactionRecordMapper, TransactionService transactionService) {
        this.accountMapper = accountMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.transactionService = transactionService;
    }

    /**
     * 计算首页概览指标；趋势率用当前月与上月同口径数据对比。
     */
    @Override
    public DashboardOverviewVO overview(YearMonth month) {
        Long userId = LoginUserContext.getUserId();
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        YearMonth previousMonth = targetMonth.minusMonths(1);

        BigDecimal totalAssets = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
                        .eq(Account::getStatus, 1))
                .stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyIncome = sumIncome(userId, targetMonth);
        BigDecimal monthlyExpense = sumExpense(userId, targetMonth);
        BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense);

        BigDecimal previousIncome = sumIncome(userId, previousMonth);
        BigDecimal previousExpense = sumExpense(userId, previousMonth);
        BigDecimal previousBalance = previousIncome.subtract(previousExpense);

        return DashboardOverviewVO.builder()
                .totalAssets(totalAssets)
                .monthlyIncome(monthlyIncome)
                .monthlyExpense(monthlyExpense)
                .monthlyBalance(monthlyBalance)
                .assetTrendRate(BigDecimal.ZERO)
                .incomeTrendRate(rate(monthlyIncome, previousIncome))
                .expenseTrendRate(rate(monthlyExpense, previousExpense))
                .balanceTrendRate(rate(monthlyBalance, previousBalance))
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
