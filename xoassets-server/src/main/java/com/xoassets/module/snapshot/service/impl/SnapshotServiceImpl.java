package com.xoassets.module.snapshot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.module.snapshot.vo.AssetSnapshotLatestVO;
import com.xoassets.module.snapshot.vo.AssetSnapshotVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.entity.AssetSnapshot;
import com.xoassets.persistence.entity.Budget;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.entity.User;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceMapper;
import com.xoassets.persistence.mapper.AssetSnapshotMapper;
import com.xoassets.persistence.mapper.BudgetMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import com.xoassets.persistence.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产快照服务实现：用当前数据库状态生成每日资产状态，用于趋势和报告。
 */
@Slf4j
@Service
public class SnapshotServiceImpl implements SnapshotService {

    private static final String TRANSACTION_INCOME = "INCOME";
    private static final String TRANSACTION_EXPENSE = "EXPENSE";
    private static final String TRANSACTION_REFUND = "REFUND";
    private static final String BUDGET_TOTAL = "TOTAL";

    private final AssetSnapshotMapper assetSnapshotMapper;
    private final AccountMapper accountMapper;
    private final HoldingMapper holdingMapper;
    private final AssetMapper assetMapper;
    private final AssetPriceMapper assetPriceMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final BudgetMapper budgetMapper;
    private final UserMapper userMapper;

    public SnapshotServiceImpl(
            AssetSnapshotMapper assetSnapshotMapper,
            AccountMapper accountMapper,
            HoldingMapper holdingMapper,
            AssetMapper assetMapper,
            AssetPriceMapper assetPriceMapper,
            TransactionRecordMapper transactionRecordMapper,
            BudgetMapper budgetMapper,
            UserMapper userMapper) {
        this.assetSnapshotMapper = assetSnapshotMapper;
        this.accountMapper = accountMapper;
        this.holdingMapper = holdingMapper;
        this.assetMapper = assetMapper;
        this.assetPriceMapper = assetPriceMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.budgetMapper = budgetMapper;
        this.userMapper = userMapper;
    }

    /**
     * 查询最新快照，并用历史快照计算昨日、本月初变化；没有历史点时返回 0。
     */
    @Override
    public AssetSnapshotLatestVO latest() {
        Long userId = LoginUserContext.getUserId();
        AssetSnapshot latest = latestSnapshot(userId);
        if (latest == null) {
            return AssetSnapshotLatestVO.builder()
                    .latest(null)
                    .netAssetChangeFromYesterday(BigDecimal.ZERO)
                    .netAssetChangeFromMonthStart(BigDecimal.ZERO)
                    .build();
        }
        AssetSnapshot yesterday = nearestSnapshotOnOrBefore(userId, latest.getSnapshotDate().minusDays(1));
        AssetSnapshot monthStart = firstSnapshotOnOrAfter(userId, latest.getSnapshotDate().withDayOfMonth(1), latest.getSnapshotDate());
        return AssetSnapshotLatestVO.builder()
                .latest(toVO(latest))
                .netAssetChangeFromYesterday(change(latest, yesterday))
                .netAssetChangeFromMonthStart(change(latest, monthStart))
                .build();
    }

    /**
     * 查询快照趋势，默认最近 30 天；只返回当前用户自己的快照。
     */
    @Override
    public List<AssetSnapshotVO> trend(LocalDate startDate, LocalDate endDate) {
        Long userId = LoginUserContext.getUserId();
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(29) : startDate;
        return assetSnapshotMapper.selectList(new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .between(AssetSnapshot::getSnapshotDate, start, end)
                        .orderByAsc(AssetSnapshot::getSnapshotDate))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 手动生成当前用户今天的快照；同一天重复调用会更新原记录。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetSnapshotVO generateToday() {
        return generateForUser(LoginUserContext.getUserId(), LocalDate.now());
    }

    /**
     * 为指定用户生成快照，所有聚合都显式带 user_id，避免跨用户数据串用。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetSnapshotVO generateForUser(Long userId, LocalDate snapshotDate) {
        LocalDate targetDate = snapshotDate == null ? LocalDate.now() : snapshotDate;
        AssetSnapshot snapshot = buildSnapshot(userId, targetDate);
        AssetSnapshot exists = assetSnapshotMapper.selectOne(new LambdaQueryWrapper<AssetSnapshot>()
                .eq(AssetSnapshot::getUserId, userId)
                .eq(AssetSnapshot::getSnapshotDate, targetDate));
        if (exists == null) {
            assetSnapshotMapper.insert(snapshot);
        } else {
            snapshot.setId(exists.getId());
            assetSnapshotMapper.update(snapshot, new LambdaUpdateWrapper<AssetSnapshot>()
                    .eq(AssetSnapshot::getId, exists.getId())
                    .eq(AssetSnapshot::getUserId, userId)
                    .eq(AssetSnapshot::getSnapshotDate, targetDate));
        }
        return toVO(snapshot);
    }

    /**
     * 定时任务批量生成快照；单个用户失败只记录日志，继续处理其他用户。
     */
    @Override
    public void generateAllUsers(LocalDate snapshotDate) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        for (User user : users) {
            try {
                generateForUser(user.getId(), snapshotDate);
            } catch (Exception exception) {
                log.error("用户资产快照生成失败，userId={}", user.getId(), exception);
            }
        }
    }

    /**
     * 按快照日期聚合资产、收支和预算，并统一保留四位小数。
     */
    private AssetSnapshot buildSnapshot(Long userId, LocalDate snapshotDate) {
        AccountSummary accountSummary = accountSummary(userId);
        InvestmentSummary investmentSummary = investmentSummary(userId);
        YearMonth month = YearMonth.from(snapshotDate);
        BigDecimal monthlyIncome = monthlyIncome(userId, month);
        BigDecimal monthlyExpense = monthlyExpense(userId, month);
        BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense).setScale(4, RoundingMode.HALF_UP);
        BudgetSummary budgetSummary = budgetSummary(userId, month);
        BigDecimal totalAsset = accountSummary.cashAsset().add(investmentSummary.marketValue()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal netAsset = totalAsset.subtract(accountSummary.liability()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal investmentProfit = investmentSummary.marketValue().subtract(investmentSummary.totalCost()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal investmentProfitRate = rate(investmentProfit, investmentSummary.totalCost());

        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setCashAsset(accountSummary.cashAsset());
        snapshot.setInvestmentAsset(investmentSummary.marketValue());
        snapshot.setTotalAsset(totalAsset);
        snapshot.setLiability(accountSummary.liability());
        snapshot.setNetAsset(netAsset);
        snapshot.setInvestmentCost(investmentSummary.totalCost());
        snapshot.setInvestmentProfit(investmentProfit);
        snapshot.setInvestmentProfitRate(investmentProfitRate);
        snapshot.setMonthlyIncome(monthlyIncome);
        snapshot.setMonthlyExpense(monthlyExpense);
        snapshot.setMonthlyBalance(monthlyBalance);
        snapshot.setBudgetUsedAmount(budgetSummary.usedAmount());
        snapshot.setBudgetTotalAmount(budgetSummary.totalAmount());
        snapshot.setBudgetUsageRate(rate(budgetSummary.usedAmount(), budgetSummary.totalAmount()));
        snapshot.setDeleted(0);
        return snapshot;
    }

    /**
     * 正余额账户计入现金资产，负余额账户按绝对值计入负债。
     */
    private AccountSummary accountSummary(Long userId) {
        BigDecimal cashAsset = BigDecimal.ZERO;
        BigDecimal liability = BigDecimal.ZERO;
        List<Account> accounts = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId)
                .eq(Account::getStatus, 1));
        for (Account account : accounts) {
            BigDecimal balance = scale4(account.getBalance());
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                cashAsset = cashAsset.add(balance);
            } else {
                liability = liability.add(balance.abs());
            }
        }
        return new AccountSummary(cashAsset.setScale(4, RoundingMode.HALF_UP), liability.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * 投资市值使用资产币种一致的最新价格快照；没有价格时用持仓平均成本兜底。
     */
    private InvestmentSummary investmentSummary(Long userId) {
        List<Holding> holdings = holdingMapper.selectList(new LambdaQueryWrapper<Holding>()
                .eq(Holding::getUserId, userId)
                .eq(Holding::getStatus, 1));
        if (holdings.isEmpty()) {
            return new InvestmentSummary(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Set<Long> assetIds = holdings.stream().map(Holding::getAssetId).collect(Collectors.toSet());
        Map<Long, Asset> assetMap = assetMapper.selectBatchIds(assetIds)
                .stream()
                .collect(Collectors.toMap(Asset::getId, asset -> asset));
        Map<Long, List<AssetPrice>> priceMap = assetPriceMapper.selectList(new LambdaQueryWrapper<AssetPrice>()
                        .in(AssetPrice::getAssetId, assetIds)
                        .orderByDesc(AssetPrice::getQuoteTime)
                        .orderByDesc(AssetPrice::getCreatedAt))
                .stream()
                .collect(Collectors.groupingBy(AssetPrice::getAssetId));

        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            Asset asset = assetMap.get(holding.getAssetId());
            BigDecimal latestPrice = latestMatchedPrice(asset, priceMap.getOrDefault(holding.getAssetId(), Collections.emptyList()));
            if (latestPrice == null) {
                latestPrice = scale4(holding.getAvgCost());
            }
            // 数量保留持仓表原始精度，避免虚拟货币小数位在快照估值前被截断。
            marketValue = marketValue.add(nullToZero(holding.getQuantity()).multiply(latestPrice).setScale(4, RoundingMode.HALF_UP));
            totalCost = totalCost.add(scale4(holding.getTotalCost()));
        }
        return new InvestmentSummary(marketValue.setScale(4, RoundingMode.HALF_UP), totalCost.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * 快照估值只使用与资产币种一致的最新价，第一版不做跨币种自动换算。
     */
    private BigDecimal latestMatchedPrice(Asset asset, List<AssetPrice> prices) {
        if (asset == null || prices.isEmpty()) {
            return null;
        }
        return prices.stream()
                .filter(price -> Objects.equals(asset.getCurrency(), price.getCurrency()))
                .max(Comparator.comparing(AssetPrice::getQuoteTime).thenComparing(AssetPrice::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AssetPrice::getPrice)
                .orElse(null);
    }

    /**
     * 月度普通收入只统计 INCOME，投资卖出和转账不进入该口径。
     */
    private BigDecimal monthlyIncome(Long userId, YearMonth month) {
        return transactionRecordMapper.selectList(monthWrapper(userId, month)
                        .eq(TransactionRecord::getType, TRANSACTION_INCOME))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 月度普通支出按 EXPENSE 减 REFUND，保持和预算、首页支出口径一致。
     */
    private BigDecimal monthlyExpense(Long userId, YearMonth month) {
        BigDecimal expense = transactionRecordMapper.selectList(monthWrapper(userId, month)
                        .eq(TransactionRecord::getType, TRANSACTION_EXPENSE))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = transactionRecordMapper.selectList(monthWrapper(userId, month)
                        .eq(TransactionRecord::getType, TRANSACTION_REFUND))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return expense.subtract(refund).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 预算总额优先取总预算，没有总预算时汇总启用的分类预算，避免双重计算。
     */
    private BudgetSummary budgetSummary(Long userId, YearMonth month) {
        List<Budget> budgets = budgetMapper.selectList(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, month.toString())
                .eq(Budget::getStatus, 1));
        BigDecimal total = budgets.stream()
                .filter(budget -> BUDGET_TOTAL.equals(budget.getBudgetType()))
                .findFirst()
                .map(Budget::getAmount)
                .orElseGet(() -> budgets.stream().map(Budget::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new BudgetSummary(monthlyExpense(userId, month), scale4(total));
    }

    /**
     * 查询用户最新快照。
     */
    private AssetSnapshot latestSnapshot(Long userId) {
        return assetSnapshotMapper.selectList(new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .orderByDesc(AssetSnapshot::getSnapshotDate)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取指定日期当天或之前最近的一条快照，用于缺失昨日快照时仍能计算变化。
     */
    private AssetSnapshot nearestSnapshotOnOrBefore(Long userId, LocalDate date) {
        return assetSnapshotMapper.selectList(new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .le(AssetSnapshot::getSnapshotDate, date)
                        .orderByDesc(AssetSnapshot::getSnapshotDate)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取本月首个快照作为月初基准，避免误用上月最后一条数据。
     */
    private AssetSnapshot firstSnapshotOnOrAfter(Long userId, LocalDate startDate, LocalDate endDate) {
        return assetSnapshotMapper.selectList(new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .between(AssetSnapshot::getSnapshotDate, startDate, endDate)
                        .orderByAsc(AssetSnapshot::getSnapshotDate)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 构造月度普通流水查询条件。
     */
    private LambdaQueryWrapper<TransactionRecord> monthWrapper(Long userId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(LocalTime.MAX);
        return new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .between(TransactionRecord::getTransactionTime, start, end);
    }

    /**
     * 计算两个快照的净资产差值。
     */
    private BigDecimal change(AssetSnapshot latest, AssetSnapshot base) {
        if (latest == null || base == null) {
            return BigDecimal.ZERO;
        }
        return scale4(latest.getNetAsset()).subtract(scale4(base.getNetAsset())).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 百分比计算统一保留四位小数，分母为 0 时返回 0。
     */
    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return scale4(numerator).multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    /**
     * 快照金额字段统一四位小数，空值按 0 处理。
     */
    private BigDecimal scale4(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 原始计算值兜底，保留传入字段自己的精度。
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 转换前端展示对象，不返回 user_id，避免泄露内部归属字段。
     */
    private AssetSnapshotVO toVO(AssetSnapshot snapshot) {
        return AssetSnapshotVO.builder()
                .id(snapshot.getId())
                .snapshotDate(snapshot.getSnapshotDate())
                .cashAsset(scale4(snapshot.getCashAsset()))
                .investmentAsset(scale4(snapshot.getInvestmentAsset()))
                .totalAsset(scale4(snapshot.getTotalAsset()))
                .liability(scale4(snapshot.getLiability()))
                .netAsset(scale4(snapshot.getNetAsset()))
                .investmentCost(scale4(snapshot.getInvestmentCost()))
                .investmentProfit(scale4(snapshot.getInvestmentProfit()))
                .investmentProfitRate(scale4(snapshot.getInvestmentProfitRate()))
                .monthlyIncome(scale4(snapshot.getMonthlyIncome()))
                .monthlyExpense(scale4(snapshot.getMonthlyExpense()))
                .monthlyBalance(scale4(snapshot.getMonthlyBalance()))
                .budgetUsedAmount(scale4(snapshot.getBudgetUsedAmount()))
                .budgetTotalAmount(scale4(snapshot.getBudgetTotalAmount()))
                .budgetUsageRate(scale4(snapshot.getBudgetUsageRate()))
                .build();
    }

    /**
     * 账户快照中间结果。
     */
    private record AccountSummary(BigDecimal cashAsset, BigDecimal liability) {
    }

    /**
     * 投资快照中间结果。
     */
    private record InvestmentSummary(BigDecimal marketValue, BigDecimal totalCost) {
    }

    /**
     * 预算快照中间结果。
     */
    private record BudgetSummary(BigDecimal usedAmount, BigDecimal totalAmount) {
    }
}
