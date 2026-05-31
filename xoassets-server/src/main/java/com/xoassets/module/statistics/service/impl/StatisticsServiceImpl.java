package com.xoassets.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.budget.service.BudgetService;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.statistics.vo.AssetDistributionVO;
import com.xoassets.module.statistics.service.StatisticsService;
import com.xoassets.module.statistics.vo.AssetTrendPointVO;
import com.xoassets.module.statistics.vo.ExpenseCategoryVO;
import com.xoassets.module.statistics.vo.IncomeExpenseTrendVO;
import com.xoassets.module.statistics.vo.InvestmentProfitTrendVO;
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
    private final HoldingService holdingService;
    private final BudgetService budgetService;

    public StatisticsServiceImpl(
            AccountMapper accountMapper,
            CategoryMapper categoryMapper,
            TransactionRecordMapper transactionRecordMapper,
            DashboardService dashboardService,
            HoldingService holdingService,
            BudgetService budgetService) {
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.dashboardService = dashboardService;
        this.holdingService = holdingService;
        this.budgetService = budgetService;
    }

    /**
     * 生成资产趋势点：用当前资产倒推历史流水影响，得到区间内每日资产估算值。
     */
    @Override
    public List<AssetTrendPointVO> assetTrend(LocalDate startDate, LocalDate endDate) {
        Long userId = LoginUserContext.getUserId();
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        BigDecimal currentAssets = currentTotalAssets(userId);

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
     * 净资产趋势第一版等同资产趋势；当前尚未建负债模型。
     */
    @Override
    public List<AssetTrendPointVO> netAssetsTrend(LocalDate startDate, LocalDate endDate) {
        return assetTrend(startDate, endDate);
    }

    /**
     * 当前账户余额加投资市值，投资无行情时由持仓服务用平均成本兜底。
     */
    private BigDecimal currentTotalAssets(Long userId) {
        BigDecimal accountAssets = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
                        .eq(Account::getStatus, 1))
                .stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investmentMarketValue = holdingService.list().stream().map(HoldingVO::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return accountAssets.add(investmentMarketValue);
    }

    /**
     * 按分类聚合月度支出，并计算每个分类在总支出中的占比。
     */
    @Override
    public List<ExpenseCategoryVO> expenseCategory(YearMonth month) {
        Long userId = LoginUserContext.getUserId();
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        List<TransactionRecord> records = transactionRecordMapper.selectList(monthWrapper(userId, targetMonth)
                .in(TransactionRecord::getType, List.of("EXPENSE", "REFUND")));
        Map<Long, Category> categoryMap = categoryMapper.selectList(new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(Category::getId, category -> category));
        Map<Long, BigDecimal> amountMap = records.stream()
                .filter(record -> record.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        TransactionRecord::getCategoryId,
                        Collectors.reducing(BigDecimal.ZERO, record -> "REFUND".equals(record.getType()) ? record.getAmount().negate() : record.getAmount(), BigDecimal::add)));
        BigDecimal total = amountMap.values().stream().map(amount -> amount.max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);

        return amountMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
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
     * 资产分布包含现金类账户和投资持仓市值。
     */
    @Override
    public List<AssetDistributionVO> assetDistribution() {
        Long userId = LoginUserContext.getUserId();
        List<AssetDistributionVO> accountItems = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
                        .eq(Account::getStatus, 1))
                .stream()
                .map(account -> AssetDistributionVO.builder()
                        .name(account.getName())
                        .type(account.getType())
                        .value(account.getBalance())
                        .percent(BigDecimal.ZERO)
                        .build())
                .toList();
        List<AssetDistributionVO> investmentItems = holdingService.list().stream()
                .map(holding -> AssetDistributionVO.builder()
                        .name(holding.getAssetName() == null ? holding.getSymbol() : holding.getAssetName())
                        .type(holding.getAssetType())
                        .value(holding.getMarketValue())
                        .percent(BigDecimal.ZERO)
                        .build())
                .toList();
        List<AssetDistributionVO> result = new ArrayList<>();
        result.addAll(accountItems);
        result.addAll(investmentItems);
        BigDecimal total = result.stream().map(AssetDistributionVO::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return result.stream()
                .map(item -> AssetDistributionVO.builder()
                        .name(item.getName())
                        .type(item.getType())
                        .value(item.getValue())
                        .percent(total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : item.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 4, RoundingMode.HALF_UP))
                        .build())
                .toList();
    }

    /**
     * 第一版投资盈亏趋势按当前持仓估值生成月度点，后续有历史持仓快照后再精细化。
     */
    @Override
    public List<InvestmentProfitTrendVO> investmentProfitTrend(YearMonth startMonth, YearMonth endMonth) {
        YearMonth start = startMonth == null ? YearMonth.now().minusMonths(5) : startMonth;
        YearMonth end = endMonth == null ? YearMonth.now() : endMonth;
        BigDecimal marketValue = holdingService.list().stream().map(HoldingVO::getMarketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = holdingService.list().stream().map(HoldingVO::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<InvestmentProfitTrendVO> result = new ArrayList<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            result.add(InvestmentProfitTrendVO.builder()
                    .month(cursor)
                    .marketValue(marketValue)
                    .totalCost(totalCost)
                    .floatingProfit(marketValue.subtract(totalCost))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    /**
     * 预算使用进度复用预算模块口径。
     */
    @Override
    public BudgetSummaryVO budgetProgress(YearMonth month) {
        YearMonth targetMonth = month == null ? YearMonth.now() : month;
        return budgetService.summary(targetMonth.toString());
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
