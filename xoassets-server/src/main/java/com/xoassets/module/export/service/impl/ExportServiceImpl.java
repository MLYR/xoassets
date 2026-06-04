package com.xoassets.module.export.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.dto.AccountLedgerQuery;
import com.xoassets.module.account.service.AccountLedgerService;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.account.vo.AccountLedgerVO;
import com.xoassets.module.export.dto.AccountLedgerExportQuery;
import com.xoassets.module.export.dto.InvestmentTransactionExportQuery;
import com.xoassets.module.export.service.ExportService;
import com.xoassets.module.transaction.dto.TransactionQuery;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * CSV 导出实现，所有数据都按当前登录 user_id 查询。
 */
@Service
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccountLedgerService accountLedgerService;
    private final AccountService accountService;
    private final TransactionRecordMapper transactionRecordMapper;
    private final InvestmentTransactionMapper investmentTransactionMapper;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final AssetMapper assetMapper;

    public ExportServiceImpl(
            AccountLedgerService accountLedgerService,
            AccountService accountService,
            TransactionRecordMapper transactionRecordMapper,
            InvestmentTransactionMapper investmentTransactionMapper,
            AccountMapper accountMapper,
            CategoryMapper categoryMapper,
            AssetMapper assetMapper) {
        this.accountLedgerService = accountLedgerService;
        this.accountService = accountService;
        this.transactionRecordMapper = transactionRecordMapper;
        this.investmentTransactionMapper = investmentTransactionMapper;
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
        this.assetMapper = assetMapper;
    }

    /**
     * 导出账户资金明细，复用账户详情同一套聚合口径。
     */
    @Override
    public ExportFile accountLedger(AccountLedgerExportQuery query) {
        Long userId = LoginUserContext.getUserId();
        accountService.findOwnedAccount(query.getAccountId(), userId);
        AccountLedgerQuery ledgerQuery = new AccountLedgerQuery();
        ledgerQuery.setPageNo(1);
        ledgerQuery.setPageSize(Integer.MAX_VALUE);
        ledgerQuery.setType(query.getType());
        ledgerQuery.setStartDate(query.getStartDate());
        ledgerQuery.setEndDate(query.getEndDate());
        ledgerQuery.setKeyword(query.getKeyword());
        List<AccountLedgerVO> rows = accountLedgerService.ledger(query.getAccountId(), ledgerQuery).getPage().getRecords();
        StringBuilder csv = new StringBuilder("时间,类型,标题,金额,账户,对方账户,分类,资产,来源,备注\n");
        rows.forEach(row -> append(csv,
                formatTime(row.getTransactionTime()),
                label(row.getBizType()),
                row.getTitle(),
                amount(row.getAmount()),
                row.getAccountName(),
                row.getRelatedAccountName(),
                row.getCategoryName(),
                assetText(row.getAssetName(), row.getSymbol()),
                row.getSourceType(),
                row.getNote()));
        return new ExportFile(filename("account-ledger"), bytes(csv));
    }

    /**
     * 导出普通流水，不包含投资买入卖出。
     */
    @Override
    public ExportFile transactions(TransactionQuery query) {
        Long userId = LoginUserContext.getUserId();
        if (query.getAccountId() != null) {
            accountService.findOwnedAccount(query.getAccountId(), userId);
        }
        List<TransactionRecord> records = transactionRecordMapper.selectList(transactionWrapper(userId, query));
        Map<Long, Account> accounts = accountMap(userId, records.stream()
                .flatMap(record -> java.util.stream.Stream.of(record.getAccountId(), record.getTargetAccountId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, Category> categories = categoryMap(userId, records.stream().map(TransactionRecord::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet()));
        StringBuilder csv = new StringBuilder("时间,类型,金额,账户,目标账户,分类,备注\n");
        records.forEach(record -> append(csv,
                formatTime(record.getTransactionTime()),
                label(record.getType()),
                amount(record.getAmount()),
                name(accounts.get(record.getAccountId())),
                name(accounts.get(record.getTargetAccountId())),
                categories.get(record.getCategoryId()) == null ? null : categories.get(record.getCategoryId()).getName(),
                record.getNote()));
        return new ExportFile(filename("transactions"), bytes(csv));
    }

    /**
     * 导出投资交易记录，包含正常和已撤销状态。
     */
    @Override
    public ExportFile investmentTransactions(InvestmentTransactionExportQuery query) {
        Long userId = LoginUserContext.getUserId();
        if (query.getAccountId() != null) {
            accountService.findOwnedAccount(query.getAccountId(), userId);
        }
        List<InvestmentTransaction> records = investmentTransactionMapper.selectList(investmentWrapper(userId, query));
        Map<Long, Account> accounts = accountMap(userId, records.stream().map(InvestmentTransaction::getAccountId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, Asset> assets = assetMap(records.stream().map(InvestmentTransaction::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet()));
        StringBuilder csv = new StringBuilder("时间,资产,代码,类型,资金账户,数量,价格,成交金额,手续费,已实现盈亏,状态,备注\n");
        records.forEach(record -> {
            Asset asset = assets.get(record.getAssetId());
            append(csv,
                    formatTime(record.getTransactionTime()),
                    asset == null ? null : asset.getName(),
                    asset == null ? null : asset.getSymbol(),
                    label(record.getType()),
                    name(accounts.get(record.getAccountId())),
                    amount(record.getQuantity()),
                    amount(record.getPrice()),
                    amount(record.getAmount()),
                    amount(record.getFee()),
                    amount(record.getRealizedProfit()),
                    label(StringUtils.hasText(record.getStatus()) ? record.getStatus() : "NORMAL"),
                    record.getNote());
        });
        return new ExportFile(filename("investment-transactions"), bytes(csv));
    }

    private LambdaQueryWrapper<TransactionRecord> transactionWrapper(Long userId, TransactionQuery query) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .orderByDesc(TransactionRecord::getTransactionTime);
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(TransactionRecord::getType, query.getType());
        }
        if (query.getAccountId() != null) {
            wrapper.and(item -> item.eq(TransactionRecord::getAccountId, query.getAccountId()).or().eq(TransactionRecord::getTargetAccountId, query.getAccountId()));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(TransactionRecord::getCategoryId, query.getCategoryId());
        }
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate(), TransactionRecord::getTransactionTime);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(TransactionRecord::getNote, query.getKeyword());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<InvestmentTransaction> investmentWrapper(Long userId, InvestmentTransactionExportQuery query) {
        LambdaQueryWrapper<InvestmentTransaction> wrapper = new LambdaQueryWrapper<InvestmentTransaction>()
                .eq(InvestmentTransaction::getUserId, userId)
                .orderByDesc(InvestmentTransaction::getTransactionTime);
        if (query.getHoldingId() != null) {
            wrapper.eq(InvestmentTransaction::getHoldingId, query.getHoldingId());
        }
        if (query.getAssetId() != null) {
            wrapper.eq(InvestmentTransaction::getAssetId, query.getAssetId());
        }
        if (query.getAccountId() != null) {
            wrapper.eq(InvestmentTransaction::getAccountId, query.getAccountId());
        }
        applyDateRange(wrapper, query.getStartDate(), query.getEndDate(), InvestmentTransaction::getTransactionTime);
        return wrapper;
    }

    private <T> void applyDateRange(LambdaQueryWrapper<T> wrapper, LocalDate startDate, LocalDate endDate, com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> column) {
        if (startDate != null) {
            wrapper.ge(column, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(column, LocalDateTime.of(endDate, LocalTime.MAX));
        }
    }

    private Map<Long, Account> accountMap(Long userId, Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectList(new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId).in(Account::getId, ids))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
    }

    private Map<Long, Category> categoryMap(Long userId, Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId).in(Category::getId, ids))
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private Map<Long, Asset> assetMap(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return assetMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Asset::getId, Function.identity()));
    }

    private void append(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values[i]));
        }
        csv.append('\n');
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String label(String type) {
        return switch (type == null ? "" : type) {
            case "INCOME" -> "收入";
            case "EXPENSE" -> "支出";
            case "TRANSFER" -> "转账";
            case "TRANSFER_IN" -> "转账转入";
            case "TRANSFER_OUT" -> "转账转出";
            case "REFUND" -> "退款";
            case "BUY", "INVEST_BUY" -> "投资买入";
            case "SELL", "INVEST_SELL" -> "投资卖出";
            case "REVOKED" -> "已撤销";
            case "NORMAL" -> "正常";
            case "CONFIRMED" -> "已确认";
            case "PENDING_CONFIRM" -> "待确认";
            case "FAILED" -> "确认失败";
            case "CANCELLED" -> "已取消";
            default -> type;
        };
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FORMATTER.format(time);
    }

    private String amount(BigDecimal value) {
        return value == null ? "" : value.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String name(Account account) {
        return account == null ? null : account.getName();
    }

    private String assetText(String assetName, String symbol) {
        if (!StringUtils.hasText(assetName)) {
            return symbol;
        }
        return StringUtils.hasText(symbol) ? assetName + " " + symbol : assetName;
    }

    private byte[] bytes(StringBuilder csv) {
        // UTF-8 BOM 让 Excel 默认按 UTF-8 打开中文 CSV。
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private String filename(String prefix) {
        return prefix + "-" + LocalDate.now() + ".csv";
    }
}
