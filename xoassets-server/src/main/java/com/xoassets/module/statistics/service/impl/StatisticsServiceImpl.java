package com.xoassets.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.statistics.service.StatisticsService;
import com.xoassets.module.statistics.vo.AssetTrendPointVO;
import com.xoassets.module.statistics.vo.ExpenseCategoryVO;
import com.xoassets.module.statistics.vo.IncomeExpenseTrendVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 基础图表统计服务：第一版用流水和账户余额实时计算。
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final DashboardService dashboardService;

    public StatisticsServiceImpl(
            AccountMapper accountMapper,
            CategoryMapper categoryMapper,
            TransactionRecordMapper transactionRecordMapper,
            DashboardService dashboardService) {
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.dashboardService = dashboardService;
    }

    /**
     * 生成资产趋势点：用当前资产倒推历史流水影响，得到区间内每日资产估算值。
     */
    @Override
    public List<AssetTrendPointVO> assetTrend(LocalDate startDate, LocalDate endDate) {
        Long userId = LoginUserContext.getUserId();
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        BigDecimal currentAssets = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
                        .eq(Account::getStatus, 1))
                .stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionRecord> records = transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .between(TransactionRecord::getTransactionTime, LocalDateTime.of(start, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX)));

        List<AssetTrendPointVO> points = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            LocalDate day = cursor;
            BigDecimal futureImpact = records.stream()
                    .filter(record -> record.getTransactionTime().toLocalDate().isAfter(day))
                    .map(this::netAssetImpact)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            points.add(AssetTrendPointVO.builder().date(day).value(currentAssets.subtract(futureImpact)).build());
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    /**
     * 按分类聚合月度支出，并计算每个分类在总支出中的占比。
     */
    @Override
    public List<ExpenseCategoryVO> expenseCategory(YearMonth month) {
        Long userId = LoginUserContext.getUserId();
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        List<TransactionRecord> expenseRecords = transactionRecordMapper.selectList(monthWrapper(userId, targetMonth)
                .eq(TransactionRecord::getType, "EXPENSE"));
        BigDecimal total = expenseRecords.stream().map(TransactionRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, Category> categoryMap = categoryMapper.selectList(new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        return expenseRecords.stream()
                .collect(Collectors.groupingBy(TransactionRecord::getCategoryId, Collectors.reducing(BigDecimal.ZERO, TransactionRecord::getAmount, BigDecimal::add)))
                .entrySet()
                .stream()
                .map(entry -> {
                    Category category = categoryMap.get(entry.getKey());
                    BigDecimal percent = total.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 4, RoundingMode.HALF_UP);
                    return ExpenseCategoryVO.builder()
                            .categoryId(entry.getKey())
                            .categoryName(category == null ? null : category.getName())
                            .amount(entry.getValue())
                            .percent(percent)
                            .build();
                })
                .toList();
    }

    /**
     * 汇总月份区间内每月收入、支出和结余趋势。
     */
    @Override
    public List<IncomeExpenseTrendVO> incomeExpenseTrend(YearMonth startMonth, YearMonth endMonth) {
        Long userId = LoginUserContext.getUserId();
        YearMonth start = startMonth == null ? YearMonth.now().minusMonths(5) : startMonth;
        YearMonth end = endMonth == null ? YearMonth.now() : endMonth;
        List<IncomeExpenseTrendVO> result = new ArrayList<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            BigDecimal income = dashboardService.sumIncome(userId, cursor);
            BigDecimal expense = dashboardService.sumExpense(userId, cursor);
            result.add(IncomeExpenseTrendVO.builder()
                    .month(cursor)
                    .income(income)
                    .expense(expense)
                    .balance(income.subtract(expense))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    /**
     * 构造指定月份的流水查询条件。
     */
    private LambdaQueryWrapper<TransactionRecord> monthWrapper(Long userId, YearMonth month) {
        return new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .between(TransactionRecord::getTransactionTime,
                        LocalDateTime.of(month.atDay(1), LocalTime.MIN),
                        LocalDateTime.of(month.atEndOfMonth(), LocalTime.MAX));
    }

    /**
     * 计算单条流水对净资产的影响；转账只改变账户分布，不影响净资产。
     */
    private BigDecimal netAssetImpact(TransactionRecord record) {
        if ("INCOME".equals(record.getType()) || "REFUND".equals(record.getType())) {
            return record.getAmount();
        }
        if ("EXPENSE".equals(record.getType())) {
            return record.getAmount().negate();
        }
        return BigDecimal.ZERO;
    }
}
