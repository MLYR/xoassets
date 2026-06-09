package com.xoassets.module.snapshot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.investment.service.InvestmentPositionHistoryService;
import com.xoassets.module.investment.service.InvestmentPositionState;
import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.module.snapshot.vo.AssetSnapshotLatestVO;
import com.xoassets.module.snapshot.vo.AssetSnapshotVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.AccountBalanceAdjustment;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.AssetSnapshot;
import com.xoassets.persistence.entity.Budget;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.entity.User;
import com.xoassets.persistence.mapper.AccountBalanceAdjustmentMapper;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.AssetSnapshotMapper;
import com.xoassets.persistence.mapper.BudgetMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import com.xoassets.persistence.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产快照服务实现：按快照日期生成资产状态，用于趋势和报告。
 */
@Slf4j
@Service
public class SnapshotServiceImpl implements SnapshotService {

    /**
     * 收入流水类型常量。
     */
    private static final String TRANSACTION_INCOME = "INCOME";
    /**
     * 支出流水类型常量。
     */
    private static final String TRANSACTION_EXPENSE = "EXPENSE";
    /**
     * 退款流水类型常量。
     */
    private static final String TRANSACTION_REFUND = "REFUND";
    /**
     * 总预算类型常量。
     */
    private static final String BUDGET_TOTAL = "TOTAL";
    /**
     * 投资买入类型常量。
     */
    private static final String INVESTMENT_TYPE_BUY = "BUY";
    /**
     * 基金金额买入录入模式常量。
     */
    private static final String INVESTMENT_INPUT_AMOUNT_NAV = "AMOUNT_NAV";
    /**
     * 投资待确认状态常量。
     */
    private static final String INVESTMENT_STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    /**
     * 投资已确认状态常量。
     */
    private static final String INVESTMENT_STATUS_CONFIRMED = "CONFIRMED";
    /**
     * 已撤销状态常量。
     */
    private static final String STATUS_REVOKED = "REVOKED";
    /**
     * 已取消状态常量。
     */
    private static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * 资产快照数据访问组件。
     */
    private final AssetSnapshotMapper assetSnapshotMapper;
    /**
     * 账户数据访问组件。
     */
    private final AccountMapper accountMapper;
    /**
     * 余额修正数据访问组件。
     */
    private final AccountBalanceAdjustmentMapper accountBalanceAdjustmentMapper;
    /**
     * 资产数据访问组件。
     */
    private final AssetMapper assetMapper;
    /**
     * 当前价格数据访问组件。
     */
    private final AssetPriceCurrentMapper assetPriceCurrentMapper;
    /**
     * 日级价格数据访问组件。
     */
    private final AssetPriceDailyMapper assetPriceDailyMapper;
    /**
     * 投资交易数据访问组件。
     */
    private final InvestmentTransactionMapper investmentTransactionMapper;
    /**
     * 流水数据访问组件。
     */
    private final TransactionRecordMapper transactionRecordMapper;
    /**
     * 预算数据访问组件。
     */
    private final BudgetMapper budgetMapper;
    /**
     * 用户数据访问组件。
     */
    private final UserMapper userMapper;
    /**
     * 持仓历史服务。
     */
    private final InvestmentPositionHistoryService positionHistoryService;

    /**
     * 注入业务依赖。
     */
    public SnapshotServiceImpl(
            AssetSnapshotMapper assetSnapshotMapper,
            AccountMapper accountMapper,
            AccountBalanceAdjustmentMapper accountBalanceAdjustmentMapper,
            AssetMapper assetMapper,
            AssetPriceCurrentMapper assetPriceCurrentMapper,
            AssetPriceDailyMapper assetPriceDailyMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            TransactionRecordMapper transactionRecordMapper,
            BudgetMapper budgetMapper,
            UserMapper userMapper,
            InvestmentPositionHistoryService positionHistoryService) {
        this.assetSnapshotMapper = assetSnapshotMapper;
        this.accountMapper = accountMapper;
        this.accountBalanceAdjustmentMapper = accountBalanceAdjustmentMapper;
        this.assetMapper = assetMapper;
        this.assetPriceCurrentMapper = assetPriceCurrentMapper;
        this.assetPriceDailyMapper = assetPriceDailyMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.budgetMapper = budgetMapper;
        this.userMapper = userMapper;
        this.positionHistoryService = positionHistoryService;
    }

    /**
     * 查询最新快照，并用历史快照计算昨日、本月初变化；缺少基准快照时返回 null，避免把缺失对比显示成 0。
     */
    @Override
    public AssetSnapshotLatestVO latest() {
        Long userId = LoginUserContext.getUserId();
        AssetSnapshot latest = latestSnapshot(userId);
        if (latest == null) {
            return AssetSnapshotLatestVO.builder()
                    .latest(null)
                    .netAssetChangeFromYesterday(null)
                    .netAssetChangeFromMonthStart(null)
                    .build();
        }
        AssetSnapshot yesterday = nearestSnapshotOnOrBefore(userId, latest.getSnapshotDate().minusDays(1));
        AssetSnapshot monthStart = firstSnapshotOnOrAfter(userId, latest.getSnapshotDate().withDayOfMonth(1), latest.getSnapshotDate());
        if (monthStart != null && monthStart.getSnapshotDate().equals(latest.getSnapshotDate()) && !latest.getSnapshotDate().equals(latest.getSnapshotDate().withDayOfMonth(1))) {
            // 本月只有最新这一天快照时没有有效月初基准，不能把月初变化冒充成 0。
            monthStart = null;
        }
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
     * 手动重建当前用户指定日期快照，用于本地对账和历史快照修复。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetSnapshotVO generate(LocalDate snapshotDate) {
        return generateForUser(LoginUserContext.getUserId(), snapshotDate == null ? LocalDate.now() : snapshotDate);
    }

    /**
     * 为指定用户生成快照，所有聚合都显式带 user_id，避免跨用户数据串用。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AssetSnapshotVO generateForUser(Long userId, LocalDate snapshotDate) {
        LocalDate targetDate = snapshotDate == null ? LocalDate.now() : snapshotDate;
        if (targetDate.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能生成未来日期资产快照");
        }
        AssetSnapshot snapshot = buildSnapshot(userId, targetDate);
        AssetSnapshot exists = assetSnapshotMapper.selectOne(new LambdaQueryWrapper<AssetSnapshot>()
                .eq(AssetSnapshot::getUserId, userId)
                .eq(AssetSnapshot::getSnapshotDate, targetDate)
                // 与 uk_user_snapshot_date(user_id, snapshot_date, deleted) 保持一致，只 upsert 当前有效快照。
                .eq(AssetSnapshot::getDeleted, 0));
        if (exists == null) {
            assetSnapshotMapper.insert(snapshot);
        } else {
            snapshot.setId(exists.getId());
            assetSnapshotMapper.update(snapshot, new LambdaUpdateWrapper<AssetSnapshot>()
                    .eq(AssetSnapshot::getId, exists.getId())
                    .eq(AssetSnapshot::getUserId, userId)
                    .eq(AssetSnapshot::getSnapshotDate, targetDate)
                    .eq(AssetSnapshot::getDeleted, 0));
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
        AccountSummary accountSummary = accountSummary(userId, snapshotDate);
        InvestmentSummary investmentSummary = investmentSummary(userId, snapshotDate);
        BigDecimal monthlyIncome = monthlyIncome(userId, snapshotDate);
        BigDecimal monthlyExpense = monthlyExpense(userId, snapshotDate);
        BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense).setScale(4, RoundingMode.HALF_UP);
        BudgetSummary budgetSummary = budgetSummary(userId, snapshotDate);
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
    private AccountSummary accountSummary(Long userId, LocalDate snapshotDate) {
        BigDecimal cashAsset = BigDecimal.ZERO;
        BigDecimal liability = BigDecimal.ZERO;
        List<Account> accounts = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId)
                .eq(Account::getStatus, 1));
        for (Account account : accounts) {
            if (account.getCreatedAt() != null && account.getCreatedAt().toLocalDate().isAfter(snapshotDate)) {
                continue;
            }
            BigDecimal balance = accountBalanceAt(userId, account, snapshotDate);
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                cashAsset = cashAsset.add(balance);
            } else {
                liability = liability.add(balance.abs());
            }
        }
        return new AccountSummary(cashAsset.setScale(4, RoundingMode.HALF_UP), liability.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * 账户历史余额按初始余额和资金事件重建，避免补跑历史资产快照时读取当前账户余额。
     */
    private BigDecimal accountBalanceAt(Long userId, Account account, LocalDate snapshotDate) {
        LocalDate start = account.getCreatedAt() == null ? LocalDate.of(1970, 1, 1) : account.getCreatedAt().toLocalDate();
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = snapshotDate.atTime(LocalTime.MAX);
        BigDecimal balance = scale4(account.getInitialBalance());
        balance = balance.add(transactionFlow(userId, account.getId(), startTime, endTime));
        balance = balance.add(investmentFlow(userId, account.getId(), startTime, endTime));
        balance = balance.add(adjustmentFlow(userId, account.getId(), start, snapshotDate));
        return balance.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算普通流水资金影响。
     */
    private BigDecimal transactionFlow(Long userId, Long accountId, LocalDateTime startTime, LocalDateTime endTime) {
        return transactionRecordMapper.selectList(new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, userId)
                        .and(item -> item.eq(TransactionRecord::getAccountId, accountId).or().eq(TransactionRecord::getTargetAccountId, accountId))
                        .ge(TransactionRecord::getTransactionTime, startTime)
                        .le(TransactionRecord::getTransactionTime, endTime))
                .stream()
                .map(record -> signedTransactionAmount(accountId, record))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算投资交易资金影响。
     */
    private BigDecimal investmentFlow(Long userId, Long accountId, LocalDateTime startTime, LocalDateTime endTime) {
        return investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .eq(InvestmentTransaction::getAccountId, accountId)
                        .ge(InvestmentTransaction::getTransactionTime, startTime)
                        .le(InvestmentTransaction::getTransactionTime, endTime))
                .stream()
                .filter(record -> !STATUS_REVOKED.equals(record.getStatus()) && !STATUS_CANCELLED.equals(record.getStatus()))
                .map(this::signedInvestmentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算余额修正资金影响。
     */
    private BigDecimal adjustmentFlow(Long userId, Long accountId, LocalDate start, LocalDate end) {
        return accountBalanceAdjustmentMapper.selectList(new LambdaQueryWrapper<AccountBalanceAdjustment>()
                        .eq(AccountBalanceAdjustment::getUserId, userId)
                        .eq(AccountBalanceAdjustment::getAccountId, accountId)
                        .between(AccountBalanceAdjustment::getBizDate, start, end))
                .stream()
                .map(AccountBalanceAdjustment::getDeltaAmount)
                .map(this::scale4)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算普通流水对账户余额的有符号影响。
     */
    private BigDecimal signedTransactionAmount(Long accountId, TransactionRecord record) {
        if ("TRANSFER".equals(record.getType())) {
            return Objects.equals(accountId, record.getTargetAccountId()) ? scale4(record.getAmount()) : scale4(record.getAmount()).negate();
        }
        if (TRANSACTION_EXPENSE.equals(record.getType())) {
            return scale4(record.getAmount()).negate();
        }
        return scale4(record.getAmount());
    }

    /**
     * 计算投资交易对账户余额的有符号影响。
     */
    private BigDecimal signedInvestmentAmount(InvestmentTransaction record) {
        if (INVESTMENT_TYPE_BUY.equals(record.getType())) {
            BigDecimal amount = record.getTradeAmount() == null ? scale4(record.getAmount()).add(scale4(record.getFee())) : scale4(record.getTradeAmount());
            return amount.negate().setScale(4, RoundingMode.HALF_UP);
        }
        return scale4(record.getAmount()).subtract(scale4(record.getFee())).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 投资市值优先使用分层行情表；待确认基金买入作为在途资产计入，避免净资产被现金扣款假性拉低。
     */
    private InvestmentSummary investmentSummary(Long userId, LocalDate snapshotDate) {
        Map<Long, InvestmentPositionState> positions = positionHistoryService.positionsAt(userId, snapshotDate);
        BigDecimal pendingAmount = pendingFundBuyAmount(userId, snapshotDate);
        if (positions.isEmpty()) {
            return new InvestmentSummary(pendingAmount, pendingAmount);
        }
        Set<Long> assetIds = positions.values().stream().map(InvestmentPositionState::assetId).collect(Collectors.toSet());
        Map<Long, Asset> assetMap = assetMapper.selectBatchIds(assetIds)
                .stream()
                .collect(Collectors.toMap(Asset::getId, asset -> asset));

        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (InvestmentPositionState position : positions.values()) {
            Asset asset = assetMap.get(position.assetId());
            BigDecimal latestPrice = latestMatchedPrice(asset, snapshotDate);
            if (latestPrice == null) {
                latestPrice = avgCost(position);
            }
            // 历史资产快照必须用交易流水重建出的日终数量和成本，不能用当前持仓倒推过去。
            marketValue = marketValue.add(nullToZero(position.quantity()).multiply(latestPrice).setScale(4, RoundingMode.HALF_UP));
            totalCost = totalCost.add(scale4(position.totalCost()));
        }
        return new InvestmentSummary(
                marketValue.add(pendingAmount).setScale(4, RoundingMode.HALF_UP),
                totalCost.add(pendingAmount).setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * 缺少历史价格时只能用该日重建成本均价兜底，避免读取当前持仓均价污染历史。
     */
    private BigDecimal avgCost(InvestmentPositionState position) {
        if (position == null || position.quantity() == null || position.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return scale4(position.totalCost()).divide(position.quantity(), 8, RoundingMode.HALF_UP);
    }

    /**
     * 快照估值以日级价为主，同日 current 可覆盖旧日线；旧价格快照表退役后缺价不再读旧审计表。
     */
    private BigDecimal latestMatchedPrice(Asset asset, LocalDate snapshotDate) {
        if (asset == null) {
            return null;
        }
        AssetPriceCurrent current = assetPriceCurrentMapper.selectById(asset.getId());
        AssetPriceDaily daily = assetPriceDailyMapper.selectOne(new LambdaQueryWrapper<AssetPriceDaily>()
                .eq(AssetPriceDaily::getAssetId, asset.getId())
                .eq(AssetPriceDaily::getCurrency, asset.getCurrency())
                .le(AssetPriceDaily::getTradeDate, snapshotDate)
                .orderByDesc(AssetPriceDaily::getTradeDate)
                .last("limit 1"));
        if (current != null && current.getPrice() != null && asset.getCurrency().equals(current.getCurrency())
                && current.getQuoteTime() != null && !current.getQuoteTime().toLocalDate().isAfter(snapshotDate)
                && (daily == null || !current.getQuoteTime().toLocalDate().isBefore(daily.getTradeDate()))) {
            // 当前价可能先于日级聚合修正，资产快照要优先使用不早于 daily 的 current，避免首页资产被旧日线带偏。
            return current.getPrice();
        }
        if (daily != null) {
            return daily.getClosePrice();
        }
        return null;
    }

    /**
     * 待确认基金买入已经扣减现金账户，快照中按在途投资资产计入总资产。
     */
    private BigDecimal pendingFundBuyAmount(Long userId, LocalDate snapshotDate) {
        LocalDateTime end = snapshotDate.atTime(LocalTime.MAX);
        return investmentTransactionMapper.selectList(new LambdaQueryWrapper<InvestmentTransaction>()
                        .eq(InvestmentTransaction::getUserId, userId)
                        .eq(InvestmentTransaction::getType, INVESTMENT_TYPE_BUY)
                        .eq(InvestmentTransaction::getInputMode, INVESTMENT_INPUT_AMOUNT_NAV)
                        .le(InvestmentTransaction::getTransactionTime, end))
                .stream()
                // 历史重放时，后来已确认的基金买入在确认日前仍是“在途投资资产”。
                .filter(transaction -> INVESTMENT_STATUS_PENDING_CONFIRM.equals(transaction.getStatus())
                        || (INVESTMENT_STATUS_CONFIRMED.equals(transaction.getStatus())
                        && transaction.getConfirmedDate() != null
                        && transaction.getConfirmedDate().isAfter(snapshotDate)))
                .map(InvestmentTransaction::getTradeAmount)
                .map(this::scale4)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 月度普通收入只统计 INCOME，投资卖出和转账不进入该口径。
     */
    private BigDecimal monthlyIncome(Long userId, LocalDate snapshotDate) {
        return transactionRecordMapper.selectList(monthToDateWrapper(userId, snapshotDate)
                        .eq(TransactionRecord::getType, TRANSACTION_INCOME))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 月度普通支出按 EXPENSE 减 REFUND，保持和预算、首页支出口径一致。
     */
    private BigDecimal monthlyExpense(Long userId, LocalDate snapshotDate) {
        BigDecimal expense = transactionRecordMapper.selectList(monthToDateWrapper(userId, snapshotDate)
                        .eq(TransactionRecord::getType, TRANSACTION_EXPENSE))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = transactionRecordMapper.selectList(monthToDateWrapper(userId, snapshotDate)
                        .eq(TransactionRecord::getType, TRANSACTION_REFUND))
                .stream()
                .map(TransactionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return expense.subtract(refund).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 预算总额优先取总预算，没有总预算时汇总启用的分类预算，避免双重计算。
     */
    private BudgetSummary budgetSummary(Long userId, LocalDate snapshotDate) {
        YearMonth month = YearMonth.from(snapshotDate);
        List<Budget> budgets = budgetMapper.selectList(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, month.toString())
                .eq(Budget::getStatus, 1));
        BigDecimal total = budgets.stream()
                .filter(budget -> BUDGET_TOTAL.equals(budget.getBudgetType()))
                .findFirst()
                .map(Budget::getAmount)
                .orElseGet(() -> budgets.stream().map(Budget::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new BudgetSummary(monthlyExpense(userId, snapshotDate), scale4(total));
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
    private LambdaQueryWrapper<TransactionRecord> monthToDateWrapper(Long userId, LocalDate snapshotDate) {
        YearMonth month = YearMonth.from(snapshotDate);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = snapshotDate.atTime(LocalTime.MAX);
        return new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .between(TransactionRecord::getTransactionTime, start, end);
    }

    /**
     * 计算两个快照的净资产差值。
     */
    private BigDecimal change(AssetSnapshot latest, AssetSnapshot base) {
        if (latest == null || base == null) {
            return null;
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
