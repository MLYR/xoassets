package com.xoassets.module.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.service.AccountOverviewService;
import com.xoassets.module.account.vo.AccountCategorySummaryVO;
import com.xoassets.module.account.vo.AccountDisplayVO;
import com.xoassets.module.account.vo.AccountOverviewVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.AssetSnapshot;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetSnapshotMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 账户总览聚合：只读取账户和资产快照，不改动账户、流水和投资业务状态。
 */
@Service
public class AccountOverviewServiceImpl implements AccountOverviewService {

    /**
     * 银行卡分组常量。
     */
    private static final String GROUP_BANK_CARD = "BANK_CARD";
    /**
     * 现金分组常量。
     */
    private static final String GROUP_CASH = "CASH";
    /**
     * 第三方账户分组常量。
     */
    private static final String GROUP_THIRD_PARTY = "THIRD_PARTY";
    /**
     * 账号脱敏匹配规则。
     */
    private static final Pattern ACCOUNT_NO_PATTERN = Pattern.compile("(\\d{4,})");

    /**
     * 账户数据访问组件。
     */
    private final AccountMapper accountMapper;
    /**
     * 资产快照数据访问组件。
     */
    private final AssetSnapshotMapper assetSnapshotMapper;

    /**
     * 注入业务依赖。
     */
    public AccountOverviewServiceImpl(AccountMapper accountMapper, AssetSnapshotMapper assetSnapshotMapper) {
        this.accountMapper = accountMapper;
        this.assetSnapshotMapper = assetSnapshotMapper;
    }

    /**
     * 聚合当前用户账户页需要的顶部资产、分类、小结和列表展示字段。
     */
    @Override
    public AccountOverviewVO overview() {
        Long userId = LoginUserContext.getUserId();
        List<Account> accounts = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId)
                .orderByAsc(Account::getCreatedAt)
                .orderByAsc(Account::getId));
        AccountTotals totals = totals(accounts);
        AssetSnapshot lastMonthSnapshot = lastMonthSnapshot(userId);
        BigDecimal lastMonthBase = lastMonthSnapshot == null ? null : netAccountAsset(lastMonthSnapshot);
        BigDecimal changeAmount = lastMonthBase == null ? BigDecimal.ZERO : totals.netAsset().subtract(lastMonthBase).setScale(4, RoundingMode.HALF_UP);
        BigDecimal changeRate = lastMonthBase == null ? BigDecimal.ZERO : rate(changeAmount, lastMonthBase.abs());

        return AccountOverviewVO.builder()
                .totalAsset(totals.netAsset())
                .lastMonthChangeAmount(changeAmount)
                .lastMonthChangeRate(changeRate)
                .compareAvailable(lastMonthBase != null)
                .accountCount(accounts.size())
                .nonCreditAssetTotal(totals.nonCreditAsset())
                .nonZeroAccountCount((int) accounts.stream().filter(account -> scale4(account.getBalance()).compareTo(BigDecimal.ZERO) != 0).count())
                .categories(categories(accounts, totals.nonCreditAsset()))
                .accounts(displayAccounts(accounts))
                .build();
    }

    /**
     * 账户页顶部资产使用账户净资产口径：正余额资产减负余额负债。
     */
    private AccountTotals totals(List<Account> accounts) {
        BigDecimal positiveAsset = BigDecimal.ZERO;
        BigDecimal liability = BigDecimal.ZERO;
        BigDecimal nonCreditAsset = BigDecimal.ZERO;
        for (Account account : accounts) {
            BigDecimal balance = scale4(account.getBalance());
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                positiveAsset = positiveAsset.add(balance);
                if (!isCreditAccount(account)) {
                    nonCreditAsset = nonCreditAsset.add(balance);
                }
            } else {
                liability = liability.add(balance.abs());
            }
        }
        return new AccountTotals(
                positiveAsset.subtract(liability).setScale(4, RoundingMode.HALF_UP),
                nonCreditAsset.setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * 分布条只统计正资产，避免信用卡负数把比例拉成负值。
     */
    private List<AccountCategorySummaryVO> categories(List<Account> accounts, BigDecimal total) {
        List<AccountCategorySummaryVO> summaries = new ArrayList<>();
        summaries.add(categorySummary(accounts, GROUP_BANK_CARD, "银行卡", "bankCard", total));
        summaries.add(categorySummary(accounts, GROUP_THIRD_PARTY, "电子钱包", "thirdParty", total));
        summaries.add(categorySummary(accounts, GROUP_CASH, "现金", "cash", total));
        return summaries;
    }

    /**
     * 构建账户分类汇总。
     */
    private AccountCategorySummaryVO categorySummary(List<Account> accounts, String group, String label, String colorKey, BigDecimal total) {
        List<Account> matched = accounts.stream().filter(account -> group(account).equals(group)).toList();
        BigDecimal amount = matched.stream()
                .map(account -> scale4(account.getBalance()))
                .filter(balance -> balance.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        return AccountCategorySummaryVO.builder()
                .group(group)
                .label(label)
                .amount(amount)
                .ratio(rate(amount, total))
                .count(matched.size())
                .colorKey(colorKey)
                .build();
    }

    /**
     * 构建账户展示列表。
     */
    private List<AccountDisplayVO> displayAccounts(List<Account> accounts) {
        Long defaultId = accounts.stream()
                .filter(account -> !isCreditAccount(account) && scale4(account.getBalance()).compareTo(BigDecimal.ZERO) >= 0)
                .min(Comparator.comparing(Account::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(Account::getId)
                .orElse(null);
        return accounts.stream().map(account -> toDisplay(account, defaultId)).toList();
    }

    /**
     * 转换为账户展示对象。
     */
    private AccountDisplayVO toDisplay(Account account, Long defaultId) {
        boolean defaultAccount = defaultId != null && defaultId.equals(account.getId());
        return AccountDisplayVO.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(scale4(account.getBalance()))
                .initialBalance(scale4(account.getInitialBalance()))
                .currency(account.getCurrency())
                .status(account.getStatus())
                .sortOrder(account.getSortOrder())
                .remark(account.getRemark())
                .displayType(displayType(account))
                .maskedNo(maskedNo(account))
                .group(group(account))
                .isDefault(defaultAccount)
                .tagText(tagText(account, defaultAccount))
                .availableCredit(null)
                .build();
    }

    /**
     * 查询上月末资产快照。
     */
    private AssetSnapshot lastMonthSnapshot(Long userId) {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate start = lastMonth.atDay(1);
        LocalDate end = lastMonth.atEndOfMonth();
        return assetSnapshotMapper.selectList(new LambdaQueryWrapper<AssetSnapshot>()
                        .eq(AssetSnapshot::getUserId, userId)
                        .between(AssetSnapshot::getSnapshotDate, start, end)
                        .orderByDesc(AssetSnapshot::getSnapshotDate))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 计算账户净资产。
     */
    private BigDecimal netAccountAsset(AssetSnapshot snapshot) {
        return scale4(snapshot.getCashAsset()).subtract(scale4(snapshot.getLiability())).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 解析账户分组。
     */
    private String group(Account account) {
        String normalized = normalized(account);
        if (normalized.contains("CASH") || normalized.contains("现金")) {
            return GROUP_CASH;
        }
        if (normalized.contains("ALIPAY") || normalized.contains("WECHAT") || normalized.contains("支付宝") || normalized.contains("微信") || normalized.contains("电子钱包") || normalized.contains("第三方")) {
            return GROUP_THIRD_PARTY;
        }
        return GROUP_BANK_CARD;
    }

    /**
     * 解析账户展示类型。
     */
    private String displayType(Account account) {
        if (isCreditAccount(account)) {
            return "信用卡";
        }
        return switch (group(account)) {
            case GROUP_CASH -> "现金账户";
            case GROUP_THIRD_PARTY -> "电子钱包";
            default -> "储蓄卡";
        };
    }

    /**
     * 生成账户标签。
     */
    private String tagText(Account account, boolean defaultAccount) {
        if (defaultAccount) {
            return group(account).equals(GROUP_CASH) ? "默认现金账户" : "默认账户";
        }
        if (group(account).equals(GROUP_THIRD_PARTY)) {
            return "已绑定";
        }
        if (isCreditAccount(account)) {
            return "账单日 --";
        }
        return null;
    }

    /**
     * 判断是否信用账户。
     */
    private boolean isCreditAccount(Account account) {
        String normalized = normalized(account);
        return normalized.contains("CREDIT") || normalized.contains("信用卡") || scale4(account.getBalance()).compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 生成脱敏账号。
     */
    private String maskedNo(Account account) {
        String source = (account.getName() == null ? "" : account.getName()) + " " + (account.getRemark() == null ? "" : account.getRemark());
        Matcher matcher = ACCOUNT_NO_PATTERN.matcher(source);
        String last = null;
        while (matcher.find()) {
            String value = matcher.group(1);
            last = value.substring(Math.max(0, value.length() - 4));
        }
        return last == null ? null : last;
    }

    /**
     * 规范化账号文本。
     */
    private String normalized(Account account) {
        return ((account.getType() == null ? "" : account.getType()) + " " + (account.getName() == null ? "" : account.getName())).toUpperCase(Locale.ROOT);
    }

    /**
     * 按金额精度保留四位小数。
     */
    private BigDecimal scale4(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算百分比。
     */
    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    /**
     * 账户资产汇总结果。
     */
    private record AccountTotals(BigDecimal netAsset, BigDecimal nonCreditAsset) {
    }
}
