package com.xoassets.module.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.api.PageResult;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.dto.AccountFlowStatisticsQuery;
import com.xoassets.module.account.dto.AccountLedgerQuery;
import com.xoassets.module.account.service.AccountLedgerService;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.account.vo.AccountFlowStatisticsVO;
import com.xoassets.module.account.vo.AccountLedgerPageVO;
import com.xoassets.module.account.vo.AccountLedgerSummaryVO;
import com.xoassets.module.account.vo.AccountLedgerVO;
import com.xoassets.module.account.vo.AccountVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 账户资金中心聚合实现，所有查询先校验账户归属再按 user_id 过滤。
 */
@Service
public class AccountLedgerServiceImpl implements AccountLedgerService {

    private static final String SOURCE_TRANSACTION = "TRANSACTION";
    private static final String SOURCE_INVESTMENT = "INVESTMENT";
    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_REVOKED = "REVOKED";

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final CategoryMapper categoryMapper;
    private final AssetMapper assetMapper;

    public AccountLedgerServiceImpl(
            AccountService accountService,
            AccountMapper accountMapper,
            TransactionRecordMapper transactionRecordMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            CategoryMapper categoryMapper,
            AssetMapper assetMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.categoryMapper = categoryMapper;
        this.assetMapper = assetMapper;
    }

    /**
     * 查询账户资金明细，普通流水和投资交易统一转换后再排序分页。
     */
    @Override
    public AccountLedgerPageVO ledger(Long accountId, AccountLedgerQuery query) {
        Long userId = LoginUserContext.getUserId();
        Account account = accountService.findOwnedAccount(accountId, userId);
        // MVP 阶段账户资金明细先在内存合并普通流水和投资交易；后续数据量变大可下沉为 SQL UNION ALL + 数据库分页。
        List<AccountLedgerVO> allRows = loadLedgerRows(userId, accountId, query);
        AccountLedgerSummaryVO summary = buildSummary(account, allRows);
        int from = Math.min((int) ((Math.max(query.getPageNo(), 1) - 1) * Math.max(query.getPageSize(), 1)), allRows.size());
        int to = Math.min(from + (int) Math.max(query.getPageSize(), 1), allRows.size());
        return AccountLedgerPageVO.builder()
                .account(toAccountVO(account))
                .summary(summary)
                .page(new PageResult<>(allRows.subList(from, to), allRows.size(), query.getPageNo(), query.getPageSize()))
                .build();
    }

    /**
     * 统计账户资金流向，投资买卖和普通收支分开汇总。
     */
    @Override
    public AccountFlowStatisticsVO flowStatistics(Long accountId, AccountFlowStatisticsQuery query) {
        Long userId = LoginUserContext.getUserId();
        accountService.findOwnedAccount(accountId, userId);
        DateRange range = resolveRange(query);
        AccountLedgerQuery ledgerQuery = new AccountLedgerQuery();
        ledgerQuery.setPageNo(1);
        ledgerQuery.setPageSize(Integer.MAX_VALUE);
        ledgerQuery.setStartDate(range.startDate());
        ledgerQuery.setEndDate(range.endDate());
        // MVP 阶段复用资金明细全量加载后聚合；后续可按分类、投资资产和日期分别改为 SQL GROUP BY 聚合。
        List<AccountLedgerVO> rows = loadLedgerRows(userId, accountId, ledgerQuery).stream()
                // 撤销投资交易保留展示，但不参与资金流向统计。
                .filter(row -> !STATUS_REVOKED.equals(row.getStatus()))
                .toList();
        BigDecimal income = sumByType(rows, "INCOME");
        BigDecimal refund = sumByType(rows, "REFUND");
        BigDecimal expense = sumByTypeAbs(rows, "EXPENSE");
        BigDecimal transferIn = sumByType(rows, "TRANSFER_IN");
        BigDecimal transferOut = sumByTypeAbs(rows, "TRANSFER_OUT");
        BigDecimal investmentBuy = sumByTypeAbs(rows, "INVEST_BUY");
        BigDecimal investmentSell = sumByType(rows, "INVEST_SELL");
        BigDecimal inflow = income.add(refund).add(transferIn).add(investmentSell);
        BigDecimal outflow = expense.add(transferOut).add(investmentBuy);
        return AccountFlowStatisticsVO.builder()
                .incomeAmount(income.add(refund))
                .expenseAmount(expense)
                .transferInAmount(transferIn)
                .transferOutAmount(transferOut)
                .investmentBuyAmount(investmentBuy)
                .investmentSellAmount(investmentSell)
                .netFlowAmount(inflow.subtract(outflow))
                .categoryExpenseStats(categoryExpenseStats(rows))
                .investmentFlowStats(investmentFlowStats(rows))
                .dailyFlowTrend(dailyFlowTrend(rows))
                .build();
    }

    /**
     * 加载统一资金明细，先查数据库再在内存合并排序。
     */
    private List<AccountLedgerVO> loadLedgerRows(Long userId, Long accountId, AccountLedgerQuery query) {
        List<TransactionRecord> transactionRecords = transactionRecordMapper.selectList(transactionWrapper(userId, accountId, query));
        List<InvestmentTransaction> investmentRecords = investmentTransactionMapper.selectList(investmentWrapper(userId, accountId, query));
        Map<Long, Account> accountMap = accountMap(userId, transactionRecords, investmentRecords);
        Map<Long, Category> categoryMap = categoryMap(userId, transactionRecords);
        Map<Long, Asset> assetMap = assetMap(investmentRecords);
        List<AccountLedgerVO> rows = new ArrayList<>();
        transactionRecords.forEach(record -> rows.add(toTransactionLedger(accountId, record, accountMap, categoryMap)));
        investmentRecords.forEach(record -> rows.add(toInvestmentLedger(record, accountMap, assetMap)));
        return rows.stream()
                .filter(row -> !StringUtils.hasText(query.getType()) || Objects.equals(row.getBizType(), query.getType()))
                .filter(row -> !StringUtils.hasText(query.getKeyword()) || matchesKeyword(row, query.getKeyword()))
                .sorted(Comparator.comparing(AccountLedgerVO::getTransactionTime).reversed())
                .toList();
    }

    /**
     * 普通流水按主账户或目标账户关联当前账户。
     */
    private LambdaQueryWrapper<TransactionRecord> transactionWrapper(Long userId, Long accountId, AccountLedgerQuery query) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .and(item -> item.eq(TransactionRecord::getAccountId, accountId).or().eq(TransactionRecord::getTargetAccountId, accountId));
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate(), TransactionRecord::getTransactionTime);
        return wrapper;
    }

    /**
     * 投资交易只按资金账户关联当前账户。
     */
    private LambdaQueryWrapper<InvestmentTransaction> investmentWrapper(Long userId, Long accountId, AccountLedgerQuery query) {
        LambdaQueryWrapper<InvestmentTransaction> wrapper = new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .eq(InvestmentTransaction::getAccountId, accountId);
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate(), InvestmentTransaction::getTransactionTime);
        return wrapper;
    }

    /**
     * 兼容 MyBatis-Plus LambdaQueryWrapper 日期区间。
     */
    private <T> void applyDateRange(LambdaQueryWrapper<T> wrapper, LocalDate startDate, LocalDate endDate, com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> column) {
        if (startDate != null) {
            wrapper.ge(column, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(column, LocalDateTime.of(endDate, LocalTime.MAX));
        }
    }

    /**
     * 转换普通流水，转账按当前账户方向拆成转出或转入。
     */
    private AccountLedgerVO toTransactionLedger(Long accountId, TransactionRecord record, Map<Long, Account> accountMap, Map<Long, Category> categoryMap) {
        Category category = record.getCategoryId() == null ? null : categoryMap.get(record.getCategoryId());
        Account account = accountMap.get(accountId);
        Account relatedAccount = relatedAccount(accountId, record, accountMap);
        String bizType = transactionBizType(accountId, record);
        BigDecimal amount = signedTransactionAmount(record, bizType);
        String title = transactionTitle(record, category, bizType);
        return AccountLedgerVO.builder()
                .id(record.getId())
                .sourceType(SOURCE_TRANSACTION)
                .bizType(bizType)
                .title(title)
                .amount(amount)
                .accountId(accountId)
                .accountName(account == null ? null : account.getName())
                .relatedAccountId(relatedAccount == null ? null : relatedAccount.getId())
                .relatedAccountName(relatedAccount == null ? null : relatedAccount.getName())
                .categoryId(record.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .status(String.valueOf(record.getStatus()))
                .transactionTime(record.getTransactionTime())
                .note(record.getNote())
                .build();
    }

    /**
     * 转换投资交易，买入为资金流出，卖出为资金流入。
     */
    private AccountLedgerVO toInvestmentLedger(InvestmentTransaction record, Map<Long, Account> accountMap, Map<Long, Asset> assetMap) {
        Asset asset = assetMap.get(record.getAssetId());
        String assetName = asset == null ? null : asset.getName();
        String symbol = asset == null ? null : asset.getSymbol();
        boolean buy = "BUY".equals(record.getType());
        BigDecimal amount = buy ? record.getAmount().add(record.getFee()).negate() : record.getAmount().subtract(record.getFee());
        String displayName = StringUtils.hasText(assetName) ? assetName : symbol;
        return AccountLedgerVO.builder()
                .id(record.getId())
                .sourceType(SOURCE_INVESTMENT)
                .bizType(buy ? "INVEST_BUY" : "INVEST_SELL")
                .title((buy ? "买入 " : "卖出 ") + (StringUtils.hasText(displayName) ? displayName : "投资资产"))
                .amount(amount)
                .accountId(record.getAccountId())
                .accountName(accountMap.get(record.getAccountId()) == null ? null : accountMap.get(record.getAccountId()).getName())
                .assetId(record.getAssetId())
                .assetName(assetName)
                .symbol(symbol)
                .status(StringUtils.hasText(record.getStatus()) ? record.getStatus() : STATUS_NORMAL)
                .transactionTime(record.getTransactionTime())
                .note(record.getNote())
                .build();
    }

    /**
     * 账户明细汇总不统计已撤销投资交易。
     */
    private AccountLedgerSummaryVO buildSummary(Account account, List<AccountLedgerVO> rows) {
        List<AccountLedgerVO> effectiveRows = rows.stream().filter(row -> !STATUS_REVOKED.equals(row.getStatus())).toList();
        BigDecimal inflow = effectiveRows.stream().map(AccountLedgerVO::getAmount).filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outflow = effectiveRows.stream().map(AccountLedgerVO::getAmount).filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        return AccountLedgerSummaryVO.builder()
                .currentBalance(account.getBalance())
                .initialBalance(account.getInitialBalance())
                .totalInflow(inflow)
                .totalOutflow(outflow)
                .netInflow(inflow.subtract(outflow))
                .transactionCount(effectiveRows.size())
                .build();
    }

    /**
     * 普通流水标题遵循分类名、备注、类型名的兜底顺序。
     */
    private String transactionTitle(TransactionRecord record, Category category, String bizType) {
        if ("TRANSFER_OUT".equals(bizType)) {
            return "转账转出";
        }
        if ("TRANSFER_IN".equals(bizType)) {
            return "转账转入";
        }
        if (category != null && StringUtils.hasText(category.getName())) {
            return category.getName();
        }
        if (StringUtils.hasText(record.getNote())) {
            return record.getNote();
        }
        return switch (record.getType()) {
            case "INCOME" -> "普通收入";
            case "EXPENSE" -> "普通支出";
            case "REFUND" -> "退款";
            default -> record.getType();
        };
    }

    /**
     * 普通流水按当前账户方向转换为统一 bizType。
     */
    private String transactionBizType(Long accountId, TransactionRecord record) {
        if ("TRANSFER".equals(record.getType())) {
            return Objects.equals(accountId, record.getTargetAccountId()) ? "TRANSFER_IN" : "TRANSFER_OUT";
        }
        return record.getType();
    }

    /**
     * 普通流水金额转成当前账户视角的有符号金额。
     */
    private BigDecimal signedTransactionAmount(TransactionRecord record, String bizType) {
        return switch (bizType) {
            case "EXPENSE", "TRANSFER_OUT" -> record.getAmount().negate();
            default -> record.getAmount();
        };
    }

    /**
     * 转账展示对方账户，非转账没有对方账户。
     */
    private Account relatedAccount(Long accountId, TransactionRecord record, Map<Long, Account> accountMap) {
        if (!"TRANSFER".equals(record.getType())) {
            return null;
        }
        Long relatedId = Objects.equals(accountId, record.getAccountId()) ? record.getTargetAccountId() : record.getAccountId();
        return relatedId == null ? null : accountMap.get(relatedId);
    }

    /**
     * 关键词匹配标题、备注、分类、资产和代码。
     */
    private boolean matchesKeyword(AccountLedgerVO row, String keyword) {
        String text = String.join(" ",
                nullToEmpty(row.getTitle()),
                nullToEmpty(row.getNote()),
                nullToEmpty(row.getCategoryName()),
                nullToEmpty(row.getAssetName()),
                nullToEmpty(row.getSymbol()));
        return text.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal sumByType(List<AccountLedgerVO> rows, String type) {
        return rows.stream().filter(row -> type.equals(row.getBizType())).map(AccountLedgerVO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByTypeAbs(List<AccountLedgerVO> rows, String type) {
        return rows.stream().filter(row -> type.equals(row.getBizType())).map(AccountLedgerVO::getAmount).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 支出分类占比只统计普通支出，不统计投资买入。
     */
    private List<AccountFlowStatisticsVO.NameAmountItem> categoryExpenseStats(List<AccountLedgerVO> rows) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        rows.stream().filter(row -> "EXPENSE".equals(row.getBizType())).forEach(row -> {
            String name = StringUtils.hasText(row.getCategoryName()) ? row.getCategoryName() : "未分类";
            grouped.merge(name, row.getAmount().abs(), BigDecimal::add);
        });
        return grouped.entrySet().stream()
                .map(entry -> AccountFlowStatisticsVO.NameAmountItem.builder().name(entry.getKey()).amount(entry.getValue()).build())
                .toList();
    }

    /**
     * 投资资金流按买入/卖出和资产名称分组。
     */
    private List<AccountFlowStatisticsVO.NameAmountItem> investmentFlowStats(List<AccountLedgerVO> rows) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        rows.stream().filter(row -> row.getBizType().startsWith("INVEST_")).forEach(row -> {
            String direction = "INVEST_BUY".equals(row.getBizType()) ? "买入 " : "卖出 ";
            String name = direction + (StringUtils.hasText(row.getAssetName()) ? row.getAssetName() : nullToEmpty(row.getSymbol()));
            grouped.merge(name.trim(), row.getAmount().abs(), BigDecimal::add);
        });
        return grouped.entrySet().stream()
                .map(entry -> AccountFlowStatisticsVO.NameAmountItem.builder().name(entry.getKey()).amount(entry.getValue()).build())
                .toList();
    }

    /**
     * 按日聚合流入、流出和净流入。
     */
    private List<AccountFlowStatisticsVO.DailyFlowItem> dailyFlowTrend(List<AccountLedgerVO> rows) {
        Map<LocalDate, List<AccountLedgerVO>> grouped = rows.stream()
                .collect(Collectors.groupingBy(row -> row.getTransactionTime().toLocalDate(), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    BigDecimal inflow = entry.getValue().stream().map(AccountLedgerVO::getAmount).filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal outflow = entry.getValue().stream().map(AccountLedgerVO::getAmount).filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return AccountFlowStatisticsVO.DailyFlowItem.builder()
                            .date(entry.getKey().toString())
                            .inflow(inflow)
                            .outflow(outflow)
                            .netFlow(inflow.subtract(outflow))
                            .build();
                })
                .toList();
    }

    /**
     * month 优先；未传日期时默认当前月，避免全量统计过大。
     */
    private DateRange resolveRange(AccountFlowStatisticsQuery query) {
        if (StringUtils.hasText(query.getMonth())) {
            YearMonth month = YearMonth.parse(query.getMonth());
            return new DateRange(month.atDay(1), month.atEndOfMonth());
        }
        if (query.getStartDate() != null || query.getEndDate() != null) {
            return new DateRange(query.getStartDate(), query.getEndDate());
        }
        YearMonth current = YearMonth.now();
        return new DateRange(current.atDay(1), current.atEndOfMonth());
    }

    private Map<Long, Account> accountMap(Long userId, List<TransactionRecord> transactions, List<InvestmentTransaction> investments) {
        Set<Long> ids = java.util.stream.Stream.concat(
                        transactions.stream().flatMap(record -> java.util.stream.Stream.of(record.getAccountId(), record.getTargetAccountId())),
                        investments.stream().map(InvestmentTransaction::getAccountId))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectList(new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId).in(Account::getId, ids))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
    }

    private Map<Long, Category> categoryMap(Long userId, List<TransactionRecord> transactions) {
        Set<Long> ids = transactions.stream().map(TransactionRecord::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId).in(Category::getId, ids))
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private Map<Long, Asset> assetMap(List<InvestmentTransaction> investments) {
        Set<Long> ids = investments.stream().map(InvestmentTransaction::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return assetMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Asset::getId, Function.identity()));
    }

    private AccountVO toAccountVO(Account account) {
        return AccountVO.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .initialBalance(account.getInitialBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .sortOrder(account.getSortOrder())
                .remark(account.getRemark())
                .build();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
