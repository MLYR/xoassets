package com.xoassets.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUser;
import com.xoassets.module.account.dto.AccountBalanceAdjustmentRequest;
import com.xoassets.module.account.service.AccountBalanceService;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.account.service.impl.AccountServiceImpl;
import com.xoassets.module.account.service.impl.AccountBalanceServiceImpl;
import com.xoassets.module.account.service.impl.AccountLedgerServiceImpl;
import com.xoassets.module.account.vo.AccountBalanceAdjustmentVO;
import com.xoassets.module.account.vo.AccountBalanceTrendPointVO;
import com.xoassets.module.account.vo.AccountFlowStatisticsVO;
import com.xoassets.module.account.vo.AccountLedgerPageVO;
import com.xoassets.module.budget.service.impl.BudgetServiceImpl;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.category.service.CategoryService;
import com.xoassets.module.dashboard.service.impl.DashboardServiceImpl;
import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
import com.xoassets.module.investment.provider.QuoteFetchResult;
import com.xoassets.module.investment.provider.QuoteProvider;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.FundConfirmDateService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.InvestmentPositionHistoryService;
import com.xoassets.module.investment.service.InvestmentPositionState;
import com.xoassets.module.investment.service.QuoteRawSnapshot;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.service.QuoteRawSnapshotService;
import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionRevokeRequest;
import com.xoassets.module.investment.dto.HoldingRequest;
import com.xoassets.module.investment.scheduler.AssetPriceDailyAggregateJob;
import com.xoassets.module.investment.scheduler.InvestmentDailySnapshotJob;
import com.xoassets.module.investment.service.impl.HoldingServiceImpl;
import com.xoassets.module.investment.service.impl.InvestmentPositionHistoryServiceImpl;
import com.xoassets.module.investment.service.impl.InvestmentTransactionServiceImpl;
import com.xoassets.module.investment.service.impl.QuoteServiceImpl;
import com.xoassets.module.investment.service.impl.RedisQuoteRawSnapshotService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.HoldingDetailVO;
import com.xoassets.module.investment.vo.HoldingSummaryVO;
import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import com.xoassets.module.investment.vo.InvestmentOverviewVO;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.module.investment.vo.InvestmentTrendVO;
import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.module.snapshot.service.impl.SnapshotServiceImpl;
import com.xoassets.module.snapshot.vo.AssetSnapshotLatestVO;
import com.xoassets.module.snapshot.vo.AssetSnapshotVO;
import com.xoassets.module.statistics.service.impl.StatisticsServiceImpl;
import com.xoassets.module.statistics.vo.AssetDistributionVO;
import com.xoassets.module.transaction.dto.TransactionRequest;
import com.xoassets.module.transaction.service.impl.TransactionServiceImpl;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.AccountBalanceAdjustment;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPriceCurrent;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.AssetSnapshot;
import com.xoassets.persistence.entity.Budget;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.MarketCalendar;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AccountBalanceAdjustmentMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.AssetSnapshotMapper;
import com.xoassets.persistence.mapper.BudgetMapper;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentDailySnapshotMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.MarketCalendarMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import com.xoassets.persistence.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * MVP 核心业务口径测试，优先覆盖余额、预算、投资估值和首页统计。
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class MvpCoreServiceTest {

    private static final Long USER_ID = 1001L;

    @BeforeEach
    void setLoginUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(USER_ID, "demo"), null));
    }

    @AfterEach
    void clearLoginUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transactionCreateAndDeleteShouldAdjustAccountBalance() {
        Account bank = account(1L, USER_ID, "银行卡", "BANK", "1000.0000");
        Account alipay = account(2L, USER_ID, "支付宝", "ALIPAY", "500.0000");
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        CategoryService categoryService = mock(CategoryService.class);
        AccountServiceImpl accountService = new AccountServiceImpl(accountMapper, transactionMapper, mock(com.xoassets.module.account.service.AccountBalanceService.class));
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                transactionMapper, accountMapper, mock(CategoryMapper.class), accountService, categoryService);

        mockAtomicBalance(accountMapper, bank, alipay);
        when(accountMapper.selectOne(any())).thenReturn(bank, bank, bank, bank, bank, alipay, bank, alipay);
        when(categoryService.findOwnedCategory(10L, USER_ID)).thenReturn(category(10L, USER_ID, "工资", "INCOME"));
        when(categoryService.findOwnedCategory(11L, USER_ID)).thenReturn(category(11L, USER_ID, "餐饮", "EXPENSE"));

        transactionService.create(transaction("INCOME", "100.0000", 1L, null, 10L));
        assertEquals(bd("1100.0000"), bank.getBalance());

        transactionService.create(transaction("EXPENSE", "30.0000", 1L, null, 11L));
        assertEquals(bd("1070.0000"), bank.getBalance());

        transactionService.create(transaction("TRANSFER", "70.0000", 1L, 2L, null));
        assertEquals(bd("1000.0000"), bank.getBalance());
        assertEquals(bd("570.0000"), alipay.getBalance());

        TransactionRecord expense = record("EXPENSE", "30.0000", 1L, null, 11L);
        expense.setId(99L);
        when(transactionMapper.selectOne(any())).thenReturn(expense);
        when(accountMapper.selectOne(any())).thenReturn(bank);
        transactionService.delete(99L);
        assertEquals(bd("1030.0000"), bank.getBalance());
    }

    @Test
    void holdingBuySellAndValuationShouldUseBigDecimalRules() {
        Holding holding = holding(1L, USER_ID, 10L, "10.0000", "10.0000", "100.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        AssetService assetService = mock(AssetService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), assetService, mock(com.xoassets.module.investment.service.InvestmentPositionHistoryService.class), quoteService);

        when(holdingMapper.update(any(), any())).thenReturn(1);
        when(holdingMapper.selectOne(any())).thenReturn(holding);
        service.applyBuy(USER_ID, 1L, 10L, bd("10.0000"), bd("20.0000"), bd("0.0000"));
        assertEquals(bd("20.0000000000"), holding.getQuantity());
        assertEquals(bd("300.0000"), holding.getTotalCost());
        assertEquals(bd("15.0000"), holding.getAvgCost());

        service.applySell(USER_ID, 1L, 10L, bd("5.0000"), bd("18.0000"), bd("0.0000"));
        assertEquals(bd("15.0000000000"), holding.getQuantity());
        assertEquals(bd("225.0000"), holding.getTotalCost());

        assertThrows(BusinessException.class, () -> service.applySell(USER_ID, 1L, 10L, bd("20.0000"), bd("18.0000"), bd("0.0000")));

        Asset asset = asset(10L, "DOGE", "Dogecoin", "CRYPTO", "USD");
        AssetPriceCurrent price = price(10L, "16.00000000", "USD");
        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price));

        HoldingVO vo = service.list().get(0);
        assertEquals(bd("240.0000"), vo.getMarketValue());
        assertEquals(bd("15.0000"), vo.getFloatingProfit());
        assertEquals(bd("6.6667"), vo.getFloatingProfitRate());
        assertEquals(6, vo.getPriceScale());
    }

    @Test
    void investmentBuyAndSellShouldLinkFundingAccount() {
        Account bank = account(1L, USER_ID, "银行卡", "BANK", "10000.0000");
        Holding holding = holding(1L, USER_ID, 10L, "0.0000", "0.0000", "0.0000");
        AccountMapper accountMapper = mock(AccountMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AssetService assetService = mock(AssetService.class);
        AccountServiceImpl accountService = new AccountServiceImpl(accountMapper, mock(TransactionRecordMapper.class), mock(com.xoassets.module.account.service.AccountBalanceService.class));
        HoldingServiceImpl holdingService = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), accountMapper, assetService, mock(com.xoassets.module.investment.service.InvestmentPositionHistoryService.class), mock(QuoteService.class));
        InvestmentTransactionServiceImpl transactionService = new InvestmentTransactionServiceImpl(
                transactionMapper, assetMapper, mock(AssetPriceDailyMapper.class), mock(AssetPriceCurrentMapper.class),
                accountMapper, assetService, mock(FundConfirmDateService.class), holdingService, accountService, mock(com.xoassets.module.snapshot.service.SnapshotService.class), mock(InvestmentDailySnapshotJob.class));

        mockAtomicBalance(accountMapper, bank);
        when(holdingMapper.update(any(), any())).thenReturn(1);
        when(accountMapper.selectOne(any())).thenReturn(bank);
        when(assetService.findAsset(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));

        transactionService.create(investmentTransaction("BUY", 1L, 10L, 1L, "100.0000", "10.0000", "2.0000"));
        assertEquals(bd("8998.0000"), bank.getBalance());
        assertEquals(bd("100.0000000000"), holding.getQuantity());
        assertEquals(bd("1002.0000"), holding.getTotalCost());
        assertEquals(bd("10.0200"), holding.getAvgCost());

        transactionService.create(investmentTransaction("BUY", 1L, 10L, 1L, "100.0000", "8.0000", "2.0000"));
        assertEquals(bd("8196.0000"), bank.getBalance());
        assertEquals(bd("200.0000000000"), holding.getQuantity());
        assertEquals(bd("1804.0000"), holding.getTotalCost());
        assertEquals(bd("9.0200"), holding.getAvgCost());

        InvestmentTransactionVO sell = transactionService.create(investmentTransaction("SELL", 1L, 10L, 1L, "50.0000", "12.0000", "2.0000"));
        assertEquals(bd("8794.0000"), bank.getBalance());
        assertEquals(bd("150.0000000000"), holding.getQuantity());
        assertEquals(bd("1353.0000"), holding.getTotalCost());
        assertEquals(bd("9.0200"), holding.getAvgCost());
        assertEquals(bd("147.0000"), sell.getRealizedProfit());
    }

    @Test
    void investmentRevokeShouldRestoreAccountAndHolding() {
        Account bank = account(1L, USER_ID, "银行卡", "BANK", "8794.0000");
        Holding holding = holding(1L, USER_ID, 10L, "150.0000", "9.0200", "1353.0000");
        AccountMapper accountMapper = mock(AccountMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AccountServiceImpl accountService = new AccountServiceImpl(accountMapper, mock(TransactionRecordMapper.class), mock(com.xoassets.module.account.service.AccountBalanceService.class));
        HoldingServiceImpl holdingService = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), accountMapper, mock(AssetService.class), mock(com.xoassets.module.investment.service.InvestmentPositionHistoryService.class), mock(QuoteService.class));
        InvestmentTransactionServiceImpl transactionService = new InvestmentTransactionServiceImpl(
                transactionMapper, assetMapper, mock(AssetPriceDailyMapper.class), mock(AssetPriceCurrentMapper.class),
                accountMapper, mock(AssetService.class), mock(FundConfirmDateService.class), holdingService, accountService, mock(com.xoassets.module.snapshot.service.SnapshotService.class), mock(InvestmentDailySnapshotJob.class));
        InvestmentTransaction sell = investmentRecord(99L, "SELL", "50.0000", "12.0000", "600.0000", "2.0000", "451.0000");

        mockAtomicBalance(accountMapper, bank);
        when(holdingMapper.update(any(), any())).thenReturn(1);
        when(transactionMapper.selectOne(any())).thenReturn(sell);
        when(accountMapper.selectOne(any())).thenReturn(bank);
        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));

        InvestmentTransactionRevokeRequest request = new InvestmentTransactionRevokeRequest();
        request.setReason("录入错误");
        InvestmentTransactionVO revoked = transactionService.revoke(99L, request);
        assertEquals(bd("8196.0000"), bank.getBalance());
        assertEquals(bd("200.0000000000"), holding.getQuantity());
        assertEquals(bd("1804.0000"), holding.getTotalCost());
        assertEquals(bd("9.0200"), holding.getAvgCost());
        assertEquals("REVOKED", revoked.getStatus());

        sell.setStatus("REVOKED");
        assertThrows(BusinessException.class, () -> transactionService.revoke(99L, request));

        InvestmentTransaction insufficientSell = investmentRecord(100L, "SELL", "50.0000", "12.0000", "600.0000", "2.0000", "451.0000");
        bank.setBalance(bd("100.0000"));
        when(transactionMapper.selectOne(any())).thenReturn(insufficientSell);
        BusinessException error = assertThrows(BusinessException.class, () -> transactionService.revoke(100L, request));
        assertEquals("账户余额不足，无法撤销该卖出交易", error.getMessage());
    }

    @Test
    void accountLedgerShouldMergeTransactionsAndInvestments() {
        Account bank = account(1L, USER_ID, "银行卡", "BANK", "10396.0000");
        Account alipay = account(2L, USER_ID, "支付宝", "ALIPAY", "1200.0000");
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentMapper = mock(InvestmentTransactionMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AccountLedgerServiceImpl service = new AccountLedgerServiceImpl(
                new AccountServiceImpl(accountMapper, transactionMapper, mock(com.xoassets.module.account.service.AccountBalanceService.class)),
                mock(com.xoassets.module.account.service.AccountBalanceService.class), accountMapper,
                mock(com.xoassets.persistence.mapper.AccountBalanceAdjustmentMapper.class), transactionMapper, investmentMapper, categoryMapper, assetMapper);

        when(accountMapper.selectOne(any())).thenReturn(bank);
        when(accountMapper.selectList(any())).thenReturn(List.of(bank, alipay));
        when(transactionMapper.selectList(any())).thenReturn(List.of(
                record("INCOME", "500.0000", 1L, null, 10L),
                record("EXPENSE", "100.0000", 1L, null, 11L),
                record("TRANSFER", "200.0000", 1L, 2L, null)));
        when(investmentMapper.selectList(any())).thenReturn(List.of(
                investmentRecord(1L, "BUY", "100.0000", "10.0000", "1000.0000", "2.0000", "1002.0000"),
                investmentRecord(2L, "SELL", "100.0000", "12.0000", "1200.0000", "2.0000", "1002.0000")));
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(10L, USER_ID, "工资", "INCOME"),
                category(11L, USER_ID, "餐饮", "EXPENSE")));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));

        AccountLedgerPageVO ledger = service.ledger(1L, new com.xoassets.module.account.dto.AccountLedgerQuery());
        assertEquals(5, ledger.getPage().getTotal());
        assertEquals(bd("1698.0000"), ledger.getSummary().getTotalInflow());
        assertEquals(bd("1302.0000"), ledger.getSummary().getTotalOutflow());
        assertEquals(bd("396.0000"), ledger.getSummary().getNetInflow());

        AccountFlowStatisticsVO flow = service.flowStatistics(1L, new com.xoassets.module.account.dto.AccountFlowStatisticsQuery());
        assertEquals(bd("500.0000"), flow.getIncomeAmount());
        assertEquals(bd("100.0000"), flow.getExpenseAmount());
        assertEquals(bd("200.0000"), flow.getTransferOutAmount());
        assertEquals(bd("1002.0000"), flow.getInvestmentBuyAmount());
        assertEquals(bd("1198.0000"), flow.getInvestmentSellAmount());
        assertEquals(1, flow.getCategoryExpenseStats().size());
        assertEquals(2, flow.getInvestmentFlowStats().size());
    }

    @Test
    void accountLedgerShouldDisplayRevokedInvestmentButExcludeItFromSummary() {
        Account bank = account(1L, USER_ID, "银行卡", "BANK", "10000.0000");
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentMapper = mock(InvestmentTransactionMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AccountLedgerServiceImpl service = new AccountLedgerServiceImpl(
                new AccountServiceImpl(accountMapper, transactionMapper, mock(com.xoassets.module.account.service.AccountBalanceService.class)),
                mock(com.xoassets.module.account.service.AccountBalanceService.class), accountMapper,
                mock(com.xoassets.persistence.mapper.AccountBalanceAdjustmentMapper.class), transactionMapper, investmentMapper, mock(CategoryMapper.class), assetMapper);
        InvestmentTransaction buy = investmentRecord(1L, "BUY", "100.0000", "10.0000", "1000.0000", "2.0000", "1002.0000");
        InvestmentTransaction revokedSell = investmentRecord(2L, "SELL", "100.0000", "12.0000", "1200.0000", "2.0000", "1002.0000");
        revokedSell.setStatus("REVOKED");

        when(accountMapper.selectOne(any())).thenReturn(bank);
        when(accountMapper.selectList(any())).thenReturn(List.of(bank));
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(investmentMapper.selectList(any())).thenReturn(List.of(buy, revokedSell));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));

        AccountLedgerPageVO ledger = service.ledger(1L, new com.xoassets.module.account.dto.AccountLedgerQuery());
        assertEquals(2, ledger.getPage().getTotal());
        assertTrue(ledger.getPage().getRecords().stream().anyMatch(row -> "REVOKED".equals(row.getStatus())));
        assertEquals(bd("0"), ledger.getSummary().getTotalInflow());
        assertEquals(bd("1002.0000"), ledger.getSummary().getTotalOutflow());
        assertEquals(bd("-1002.0000"), ledger.getSummary().getNetInflow());
    }

    @Test
    void accountBalanceAdjustmentShouldCreateAuditEventAndBalanceTrend() {
        Account bank = account(1L, USER_ID, "银行卡", "BANK", "1000.0000");
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentMapper = mock(InvestmentTransactionMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        List<AccountBalanceAdjustment> adjustments = new java.util.ArrayList<>();
        AccountBalanceServiceImpl service = new AccountBalanceServiceImpl(accountMapper, transactionMapper, investmentMapper, adjustmentMapper);

        mockAtomicBalance(accountMapper, bank);
        when(accountMapper.selectOwnedForUpdate(USER_ID, 1L)).thenReturn(bank);
        when(accountMapper.selectById(1L)).thenReturn(bank);
        when(accountMapper.selectOne(any())).thenReturn(bank);
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(investmentMapper.selectList(any())).thenReturn(List.of());
        when(adjustmentMapper.insert(any(AccountBalanceAdjustment.class))).thenAnswer(invocation -> {
            AccountBalanceAdjustment adjustment = invocation.getArgument(0);
            adjustments.add(adjustment);
            return 1;
        });
        when(adjustmentMapper.selectList(any())).thenAnswer(invocation -> adjustments);

        AccountBalanceAdjustmentRequest request = new AccountBalanceAdjustmentRequest();
        request.setAfterBalance(bd("1123.4500"));
        request.setReason("对账修正");
        request.setBizDate(LocalDate.of(2026, 5, 2));
        AccountBalanceAdjustmentVO adjustment = service.adjustBalance(1L, request);

        assertEquals(bd("123.4500"), adjustment.getDeltaAmount());
        assertEquals(bd("1123.4500"), bank.getBalance());
        List<AccountBalanceTrendPointVO> trend = service.balanceTrend(1L, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 2));
        assertEquals(bd("1123.4500"), trend.get(0).getEndBalance());
        assertEquals(bd("123.4500"), trend.get(0).getAdjustmentAmount());
    }

    @Test
    void accountLedgerShouldIncludeBalanceAdjustmentRows() {
        Account bank = account(1L, USER_ID, "银行卡", "BANK", "1050.0000");
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentMapper = mock(InvestmentTransactionMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        AccountBalanceService accountBalanceService = mock(AccountBalanceService.class);
        AccountLedgerServiceImpl service = new AccountLedgerServiceImpl(
                new AccountServiceImpl(accountMapper, transactionMapper, mock(AccountBalanceService.class)),
                accountBalanceService, accountMapper, adjustmentMapper, transactionMapper, investmentMapper, mock(CategoryMapper.class), mock(AssetMapper.class));

        when(accountMapper.selectOne(any())).thenReturn(bank);
        when(accountMapper.selectList(any())).thenReturn(List.of(bank));
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(investmentMapper.selectList(any())).thenReturn(List.of());
        when(adjustmentMapper.selectList(any())).thenReturn(List.of(adjustment(1L, 1L, "50.0000", LocalDate.of(2026, 5, 1))));
        when(accountBalanceService.balanceTrend(any(), any(), any())).thenReturn(List.of(
                AccountBalanceTrendPointVO.builder()
                        .date("2026-05-01")
                        .endBalance(bd("1050.0000"))
                        .inflow(bd("50.0000"))
                        .outflow(bd("0.0000"))
                        .adjustmentAmount(bd("50.0000"))
                        .build()));

        AccountLedgerPageVO ledger = service.ledger(1L, new com.xoassets.module.account.dto.AccountLedgerQuery());
        assertEquals(1, ledger.getPage().getTotal());
        assertEquals("ADJUSTMENT", ledger.getPage().getRecords().get(0).getSourceType());
        assertEquals("BALANCE_ADJUSTMENT", ledger.getPage().getRecords().get(0).getBizType());

        AccountFlowStatisticsVO flow = service.flowStatistics(1L, new com.xoassets.module.account.dto.AccountFlowStatisticsQuery());
        assertEquals(bd("50.0000"), flow.getAdjustmentAmount());
        assertEquals(1, flow.getDailyBalanceTrend().size());
    }

    @Test
    void investmentPositionHistoryShouldReplayTransactionsForPastDate() {
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentDailySnapshotMapper dailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        InvestmentPositionHistoryServiceImpl service = new InvestmentPositionHistoryServiceImpl(holdingMapper, transactionMapper, dailySnapshotMapper);
        InvestmentTransaction buy = investmentRecord(1L, "BUY", "100.0000", "10.0000", "1000.0000", "0.0000", "1000.0000");
        buy.setTransactionTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        InvestmentTransaction sell = investmentRecord(2L, "SELL", "40.0000", "12.0000", "480.0000", "2.0000", "400.0000");
        sell.setTransactionTime(LocalDateTime.of(2026, 5, 3, 10, 0));

        when(transactionMapper.selectList(any())).thenReturn(List.of(buy, sell));
        when(holdingMapper.selectList(any())).thenReturn(List.of());
        when(dailySnapshotMapper.selectList(any())).thenReturn(List.of());

        InvestmentPositionState beforeSell = service.positionsAt(USER_ID, LocalDate.of(2026, 5, 2)).get(1L);
        assertEquals(bd("100.0000000000"), beforeSell.quantity());
        assertEquals(bd("1000.0000"), beforeSell.totalCost());

        InvestmentPositionState afterSell = service.positionsAt(USER_ID, LocalDate.of(2026, 5, 3)).get(1L);
        assertEquals(bd("60.0000000000"), afterSell.quantity());
        assertEquals(bd("600.0000"), afterSell.totalCost());
        assertEquals(bd("-478.0000"), service.netInflow(USER_ID, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 3)));
    }

    @Test
    void investmentPositionHistoryShouldKeepManualBaseWhenHoldingHasFundBuyTransaction() {
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentDailySnapshotMapper dailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        InvestmentPositionHistoryServiceImpl service = new InvestmentPositionHistoryServiceImpl(holdingMapper, transactionMapper, dailySnapshotMapper);
        Holding holding = holding(1L, USER_ID, 10L, "120.0000", "10.0000", "1200.0000");
        holding.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        InvestmentTransaction fundBuy = investmentRecord(1L, "BUY", "20.0000", "10.0000", "200.0000", "0.0000", "200.0000");
        fundBuy.setInputMode("AMOUNT_NAV");
        fundBuy.setStatus("CONFIRMED");
        fundBuy.setTransactionTime(LocalDateTime.of(2026, 6, 4, 9, 30));
        fundBuy.setConfirmedDate(LocalDate.of(2026, 6, 8));

        when(transactionMapper.selectList(any())).thenReturn(List.of(fundBuy));
        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(dailySnapshotMapper.selectList(any())).thenReturn(List.of());

        InvestmentPositionState beforeConfirm = service.positionsAt(USER_ID, LocalDate.of(2026, 6, 4)).get(1L);
        InvestmentPositionState afterConfirm = service.positionsAt(USER_ID, LocalDate.of(2026, 6, 8)).get(1L);

        // 手工初始化底仓不能因为同持仓后续有基金金额买入流水而从历史快照中消失。
        assertEquals(bd("100.0000000000"), beforeConfirm.quantity());
        assertEquals(bd("1000.0000"), beforeConfirm.totalCost());
        assertEquals(bd("120.0000000000"), afterConfirm.quantity());
        assertEquals(bd("1200.0000"), afterConfirm.totalCost());
    }

    @Test
    void confirmedFundBuyShouldKeepTenDecimalQuantity() {
        Holding holding = holding(1L, USER_ID, 10L, "0.0000", "0.0000", "0.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, mock(AssetMapper.class), mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));

        when(holdingMapper.update(any(), any())).thenReturn(1);
        when(holdingMapper.selectOne(any())).thenReturn(holding);

        service.applyConfirmedBuy(USER_ID, 1L, 10L, bd("12.3456789012"), bd("100.0000"));
        assertEquals(bd("12.3456789012"), holding.getQuantity());
    }

    @Test
    void holdingDeleteShouldRejectNonZeroQuantity() {
        Holding holding = holding(1L, USER_ID, 10L, "1.0000", "10.0000", "10.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, mock(AssetMapper.class), mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);

        // 未清仓持仓必须后端兜底拒绝删除，不能只依赖 Web 隐藏删除按钮。
        assertThrows(BusinessException.class, () -> service.delete(1L));
        verify(holdingMapper, never()).delete(any());
    }

    @Test
    void holdingDeleteShouldAllowZeroQuantity() {
        Holding holding = holding(1L, USER_ID, 10L, "0.0000", "0.0000", "0.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, mock(AssetMapper.class), mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);

        service.delete(1L);

        verify(holdingMapper).delete(any());
    }

    @Test
    void fundAmountBuyShouldConfirmWithDailyNavBeforeCurrentPriceFallback() {
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetService assetService = mock(AssetService.class);
        FundConfirmDateService confirmDateService = mock(FundConfirmDateService.class);
        HoldingService holdingService = mock(HoldingService.class);
        AccountService accountService = mock(AccountService.class);
        SnapshotService snapshotService = mock(SnapshotService.class);
        InvestmentDailySnapshotJob investmentDailySnapshotJob = mock(InvestmentDailySnapshotJob.class);
        InvestmentTransactionServiceImpl service = new InvestmentTransactionServiceImpl(
                transactionMapper, assetMapper, dailyMapper, currentMapper, mock(AccountMapper.class),
                assetService, confirmDateService, holdingService, accountService, snapshotService, investmentDailySnapshotJob);
        Asset fund = asset(10L, "FUND-A", "基金 A", "FUND", "CNY");
        Account account = account(1L, USER_ID, "银行卡", "BANK", "9999.0000");
        Holding holding = holding(1L, USER_ID, 10L, "0.0000", "0.0000", "0.0000");
        InvestmentTransactionRequest request = investmentTransaction("BUY", null, 10L, 1L, "1.0000", "1.0000", "1.0000");
        request.setInputMode("AMOUNT_NAV");
        request.setTradeAmount(bd("1000.0000"));
        request.setTransactionTime(LocalDateTime.of(2026, 6, 4, 9, 30));

        when(assetService.findAsset(10L)).thenReturn(fund);
        when(accountService.findOwnedAccount(1L, USER_ID)).thenReturn(account);
        when(confirmDateService.effectiveTradeDate(fund, request.getTransactionTime())).thenReturn(LocalDate.of(2026, 6, 4));
        when(confirmDateService.confirmedDate(fund, request.getTransactionTime())).thenReturn(LocalDate.of(2026, 6, 8));
        when(dailyMapper.selectOne(any())).thenReturn(dailyPrice(10L, LocalDate.of(2026, 6, 4), "1.25000000", "CNY"));
        when(holdingService.applyConfirmedBuy(any(), any(), any(), any(), any())).thenReturn(new com.xoassets.module.investment.service.HoldingTradeResult(holding, null, null));

        service.create(request);

        ArgumentCaptor<InvestmentTransaction> captor = ArgumentCaptor.forClass(InvestmentTransaction.class);
        verify(transactionMapper).insert(captor.capture());
        InvestmentTransaction inserted = captor.getValue();
        // 基金份额按有效申请日净值确认，确认日期只作为到账/状态日期保存。
        assertEquals("CONFIRMED", inserted.getStatus());
        assertEquals(bd("1.250000"), inserted.getConfirmedNav());
        assertEquals(LocalDate.of(2026, 6, 8), inserted.getConfirmedDate());
        verify(currentMapper, never()).selectById(any());
        verify(investmentDailySnapshotJob).snapshotForUser(USER_ID, LocalDate.of(2026, 6, 4));
        verify(investmentDailySnapshotJob).snapshotForUser(USER_ID, LocalDate.of(2026, 6, 8));
        verify(snapshotService).generateForUser(USER_ID, LocalDate.of(2026, 6, 4));
    }

    @Test
    void pendingFundBuyShouldConfirmWithCurrentPriceOnlyWhenQuoteDateMatchesEffectiveTradeDate() {
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        FundConfirmDateService confirmDateService = mock(FundConfirmDateService.class);
        HoldingService holdingService = mock(HoldingService.class);
        SnapshotService snapshotService = mock(SnapshotService.class);
        InvestmentDailySnapshotJob investmentDailySnapshotJob = mock(InvestmentDailySnapshotJob.class);
        InvestmentTransactionServiceImpl service = new InvestmentTransactionServiceImpl(
                transactionMapper, assetMapper, dailyMapper, currentMapper, mock(AccountMapper.class),
                mock(AssetService.class), confirmDateService, holdingService, mock(AccountService.class), snapshotService, investmentDailySnapshotJob);
        InvestmentTransaction pending = pendingFundBuy(LocalDate.of(2026, 6, 8));
        Asset fund = asset(10L, "FUND-A", "QDII 基金", "FUND", "CNY");
        AssetPriceCurrent current = price(10L, "1.30000000", "CNY", LocalDateTime.of(2026, 6, 4, 21, 30));

        when(transactionMapper.selectList(any())).thenReturn(List.of(pending));
        when(assetMapper.selectById(10L)).thenReturn(fund);
        when(confirmDateService.effectiveTradeDate(fund, pending.getTransactionTime())).thenReturn(LocalDate.of(2026, 6, 4));
        when(dailyMapper.selectOne(any())).thenReturn(null);
        when(currentMapper.selectById(10L)).thenReturn(current);
        when(transactionMapper.update(any(), any())).thenReturn(1);
        when(holdingService.applyConfirmedBuy(any(), any(), any(), any(), any())).thenReturn(new com.xoassets.module.investment.service.HoldingTradeResult(holding(1L, USER_ID, 10L, "0.0000", "0.0000", "0.0000"), null, null));

        service.confirmPendingFundBuys();

        ArgumentCaptor<InvestmentTransaction> captor = ArgumentCaptor.forClass(InvestmentTransaction.class);
        verify(transactionMapper).update(captor.capture(), any());
        InvestmentTransaction updated = captor.getValue();
        // current 只能作为“有效申请日净值已入 current、daily 尚未聚合”的短暂兜底。
        assertEquals("CONFIRMED", updated.getStatus());
        assertEquals(bd("1.300000"), updated.getConfirmedNav());
        verify(holdingService).applyConfirmedBuy(eq(USER_ID), eq(1L), eq(10L), any(), eq(bd("1000.0000")));
        verify(investmentDailySnapshotJob).snapshotForUser(USER_ID, LocalDate.of(2026, 6, 4));
        verify(investmentDailySnapshotJob).snapshotForUser(USER_ID, LocalDate.of(2026, 6, 8));
        verify(snapshotService).generateForUser(USER_ID, LocalDate.of(2026, 6, 4));
        verify(snapshotService).generateForUser(USER_ID, LocalDate.of(2026, 6, 8));
    }

    @Test
    void pendingFundBuyShouldIgnoreStaleCurrentPriceAndRemainPending() {
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        FundConfirmDateService confirmDateService = mock(FundConfirmDateService.class);
        HoldingService holdingService = mock(HoldingService.class);
        InvestmentTransactionServiceImpl service = new InvestmentTransactionServiceImpl(
                transactionMapper, assetMapper, dailyMapper, currentMapper, mock(AccountMapper.class),
                mock(AssetService.class), confirmDateService, holdingService, mock(AccountService.class), mock(SnapshotService.class), mock(InvestmentDailySnapshotJob.class));
        InvestmentTransaction pending = pendingFundBuy(LocalDate.of(2026, 6, 8));
        Asset fund = asset(10L, "FUND-A", "QDII 基金", "FUND", "CNY");
        AssetPriceCurrent staleCurrent = price(10L, "1.30000000", "CNY", LocalDateTime.of(2026, 6, 7, 21, 30));

        when(transactionMapper.selectList(any())).thenReturn(List.of(pending));
        when(assetMapper.selectById(10L)).thenReturn(fund);
        when(confirmDateService.effectiveTradeDate(fund, pending.getTransactionTime())).thenReturn(LocalDate.of(2026, 6, 4));
        when(dailyMapper.selectOne(any())).thenReturn(null);
        when(currentMapper.selectById(10L)).thenReturn(staleCurrent);

        service.confirmPendingFundBuys();

        // 有效申请日不匹配时不能拿 current 表旧净值冒充确认净值，交易继续待确认。
        verify(transactionMapper, never()).update(any(), any());
        verify(holdingService, never()).applyConfirmedBuy(any(), any(), any(), any(), any());
    }

    @Test
    void holdingProfitAnalysisShouldUseLatestAndHistoricalPrices() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, quoteService);
        Asset asset = asset(10L, "FUND-A", "基金 A", "FUND", "CNY");
        AssetPriceCurrent latest = price(10L, "11.00000000", "CNY", LocalDateTime.now());
        AssetPriceDaily previous = dailyPrice(10L, LocalDate.now().minusDays(1), "9.00000000", "CNY");
        AssetPriceDaily beforePrevious = dailyPrice(10L, LocalDate.now().minusDays(2), "8.00000000", "CNY");

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, latest));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(previous, beforePrevious));
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(holding.getQuantity());

        HoldingVO vo = service.list().get(0);
        assertEquals(bd("1100.0000"), vo.getMarketValue());
        assertEquals(bd("100.0000"), vo.getFloatingProfit());
        assertEquals(bd("10.0000"), vo.getFloatingProfitRate());
        assertEquals(bd("200.0000"), vo.getTodayProfit());
        assertEquals(bd("22.2222"), vo.getTodayChangeRate());
        assertEquals(bd("100.0000"), vo.getYesterdayProfit());
        assertEquals(bd("12.5000"), vo.getYesterdayChangeRate());
        assertEquals(bd("0"), vo.getBreakEvenRate());

        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "8.00000000", "CNY", LocalDateTime.now())));
        HoldingVO loss = service.list().get(0);
        assertEquals(bd("25.0000"), loss.getBreakEvenRate());
    }

    @Test
    void holdingSummaryShouldUseInvestmentDailySnapshotForVsYesterday() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentDailySnapshotMapper investmentDailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                investmentDailySnapshotMapper, mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(com.xoassets.module.investment.service.InvestmentPositionHistoryService.class), quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.now())));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(dailyPrice(10L, LocalDate.now().minusDays(1), "10.00000000", "CNY")));
        when(investmentDailySnapshotMapper.selectList(any()))
                .thenReturn(List.of(investmentDailySnapshot(USER_ID, LocalDate.now().minusDays(1), "1000.0000")))
                .thenReturn(List.of());

        assertEquals(bd("100.0000"), service.summary().getTodayProfit());
    }

    @Test
    void investmentOverviewShouldKeepTodayProfitUnavailableWhenTodayPriceIsMissing() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.now().minusDays(1))));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(dailyPrice(10L, LocalDate.now().minusDays(1), "10.00000000", "CNY")));
        when(marketCalendarMapper.selectList(any())).thenReturn(List.of(marketCalendar(LocalDate.now().minusDays(1), true, "MANUAL")));

        InvestmentOverviewVO overview = service.overview();
        // 旧净值没有映射到今天展示时，总览和模块卡都返回 null，让 Web 显示 -- 而不是 ¥0.0000。
        assertEquals(false, overview.getTodayProfitAvailable());
        assertEquals(null, overview.getTodayProfit());
        assertEquals("今日净值未更新", overview.getTodayProfitStatusLabel());
        assertEquals(false, overview.getModuleAssets().get(0).getPrimaryProfitAvailable());
        assertEquals(null, overview.getModuleAssets().get(0).getPrimaryProfitAmount());
        assertEquals("今日净值未更新", overview.getModuleAssets().get(0).getPrimaryProfitStatusLabel());
    }

    @Test
    void investmentOverviewShouldExposeMarketClosedReasonWhenFundIsClosed() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.now())));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(dailyPrice(10L, LocalDate.now().minusDays(1), "10.00000000", "CNY")));
        // 总览和模块卡片也要返回休市原因，不能只在收益日历里显示。
        when(marketCalendarMapper.selectOne(any())).thenReturn(marketCalendar(LocalDate.now(), false, "EXCHANGE_ANNOUNCEMENT"));

        InvestmentOverviewVO overview = service.overview();
        assertEquals(false, overview.getTodayProfitAvailable());
        assertEquals(null, overview.getTodayProfit());
        assertEquals("今日休市", overview.getTodayProfitStatusLabel());
        assertEquals(false, overview.getModuleAssets().get(0).getPrimaryProfitAvailable());
        assertEquals("今日休市", overview.getModuleAssets().get(0).getPrimaryProfitStatusLabel());
    }

    @Test
    void holdingSummaryShouldKeepTodayProfitUnavailableWhenTodayPriceIsMissing() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.now().minusDays(1))));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(dailyPrice(10L, LocalDate.now().minusDays(1), "10.00000000", "CNY")));
        when(marketCalendarMapper.selectList(any())).thenReturn(List.of(marketCalendar(LocalDate.now().minusDays(1), true, "MANUAL")));

        HoldingSummaryVO summary = service.summary();
        // 持仓汇总接口同样不能把未映射到今天的旧净值展示成 0。
        assertEquals(false, summary.getTodayProfitAvailable());
        assertEquals(null, summary.getTodayProfit());
    }

    @Test
    void latestSnapshotShouldReturnNullChangeWhenYesterdayBaselineIsMissing() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, mock(AccountMapper.class), mock(AccountBalanceAdjustmentMapper.class), mock(AssetMapper.class),
                mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), mock(InvestmentTransactionMapper.class),
                mock(TransactionRecordMapper.class), mock(BudgetMapper.class), mock(UserMapper.class), mock(InvestmentPositionHistoryService.class));
        AssetSnapshot latest = assetSnapshot(1L, USER_ID, LocalDate.of(2026, 6, 8), "10000.0000");

        when(assetSnapshotMapper.selectList(any()))
                .thenReturn(List.of(latest))
                .thenReturn(List.of())
                .thenReturn(List.of(latest));

        AssetSnapshotLatestVO vo = service.latest();
        // 缺少昨日基准快照时返回 null，让首页展示 --，不能把缺失比较冒充为 0。
        assertEquals(null, vo.getNetAssetChangeFromYesterday());
        assertEquals(null, vo.getNetAssetChangeFromMonthStart());
    }

    @Test
    void latestSnapshotShouldAllowZeroMonthChangeOnMonthFirstDay() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, mock(AccountMapper.class), mock(AccountBalanceAdjustmentMapper.class), mock(AssetMapper.class),
                mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), mock(InvestmentTransactionMapper.class),
                mock(TransactionRecordMapper.class), mock(BudgetMapper.class), mock(UserMapper.class), mock(InvestmentPositionHistoryService.class));
        AssetSnapshot latest = assetSnapshot(1L, USER_ID, LocalDate.of(2026, 6, 1), "10000.0000");

        when(assetSnapshotMapper.selectList(any()))
                .thenReturn(List.of(latest))
                .thenReturn(List.of())
                .thenReturn(List.of(latest));

        AssetSnapshotLatestVO vo = service.latest();
        // 最新快照本身就是 1 号时，月初变化为 0 是真实口径，不是缺失基准。
        assertEquals(bd("0.0000"), vo.getNetAssetChangeFromMonthStart());
    }

    @Test
    void assetSnapshotShouldUseHistoricalInvestmentPositionForPastDate() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionRecordMapper = mock(TransactionRecordMapper.class);
        BudgetMapper budgetMapper = mock(BudgetMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, accountMapper, adjustmentMapper, assetMapper,
                mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper, investmentTransactionMapper,
                transactionRecordMapper, budgetMapper, mock(UserMapper.class), positionHistoryService);

        LocalDate snapshotDate = LocalDate.of(2026, 6, 5);
        when(assetSnapshotMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectList(any())).thenReturn(List.of());
        when(adjustmentMapper.selectList(any())).thenReturn(List.of());
        when(transactionRecordMapper.selectList(any())).thenReturn(List.of());
        when(budgetMapper.selectList(any())).thenReturn(List.of());
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(assetPriceDailyMapper.selectOne(any())).thenReturn(dailyPrice(10L, snapshotDate, "11.00000000", "CNY"));
        when(positionHistoryService.positionsAt(USER_ID, snapshotDate)).thenReturn(Map.of(
                1L, new InvestmentPositionState(1L, 10L, bd("100.0000000000"), bd("1000.0000"))));
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of());

        AssetSnapshotVO snapshot = service.generateForUser(USER_ID, snapshotDate);
        // 过去日期资产快照必须按当日重建份额估值，不能用当前持仓表里的最新数量。
        assertEquals(bd("1100.0000"), snapshot.getInvestmentAsset());
        assertEquals(bd("1000.0000"), snapshot.getInvestmentCost());
        assertEquals(bd("100.0000"), snapshot.getInvestmentProfit());
    }

    @Test
    void assetSnapshotShouldUseCurrentPriceWhenDailyIsStaleForQuoteDate() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionRecordMapper = mock(TransactionRecordMapper.class);
        BudgetMapper budgetMapper = mock(BudgetMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, accountMapper, adjustmentMapper, assetMapper,
                currentMapper, dailyMapper, investmentTransactionMapper,
                transactionRecordMapper, budgetMapper, mock(UserMapper.class), positionHistoryService);

        LocalDate snapshotDate = LocalDate.now();
        Asset stock = asset(10L, "600666.SH", "奥瑞德", "STOCK", "CNY");
        when(assetSnapshotMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectList(any())).thenReturn(List.of());
        when(adjustmentMapper.selectList(any())).thenReturn(List.of());
        when(transactionRecordMapper.selectList(any())).thenReturn(List.of());
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of());
        when(budgetMapper.selectList(any())).thenReturn(List.of());
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(stock));
        when(dailyMapper.selectOne(any())).thenReturn(dailyPrice(10L, snapshotDate, "11.00000000", "CNY"));
        when(currentMapper.selectById(10L)).thenReturn(price(10L, "12.00000000", "CNY", snapshotDate.atTime(15, 0)));
        when(positionHistoryService.positionsAt(USER_ID, snapshotDate)).thenReturn(Map.of(
                1L, new InvestmentPositionState(1L, 10L, bd("100.0000"), bd("1000.0000"))));

        AssetSnapshotVO snapshot = service.generateForUser(USER_ID, snapshotDate);
        // 总资产快照也要兜住同日旧 daily，否则首页投资资产会继续被旧价污染。
        assertEquals(bd("1200.0000"), snapshot.getInvestmentAsset());
        assertEquals(bd("200.0000"), snapshot.getInvestmentProfit());
    }

    @Test
    void assetSnapshotShouldRebuildHistoricalAccountBalanceForPastDate() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        TransactionRecordMapper transactionRecordMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        BudgetMapper budgetMapper = mock(BudgetMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, accountMapper, adjustmentMapper, mock(AssetMapper.class),
                mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), investmentTransactionMapper,
                transactionRecordMapper, budgetMapper, mock(UserMapper.class), positionHistoryService);
        Account account = account(1L, USER_ID, "银行卡", "BANK", "9999.0000");
        account.setInitialBalance(bd("1000.0000"));
        account.setCreatedAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        TransactionRecord income = record("INCOME", "200.0000", 1L, null, 10L);
        income.setTransactionTime(LocalDateTime.of(2026, 6, 2, 10, 0));
        TransactionRecord expense = record("EXPENSE", "50.0000", 1L, null, 11L);
        expense.setTransactionTime(LocalDateTime.of(2026, 6, 3, 10, 0));
        InvestmentTransaction buy = investmentRecord(1L, "BUY", "10.0000", "10.0000", "100.0000", "0.0000", "100.0000");
        buy.setTransactionTime(LocalDateTime.of(2026, 6, 3, 11, 0));
        AccountBalanceAdjustment adjustment = adjustment(1L, 1L, "25.0000", LocalDate.of(2026, 6, 3));
        LocalDate snapshotDate = LocalDate.of(2026, 6, 3);

        when(assetSnapshotMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectList(any())).thenReturn(List.of(account));
        when(transactionRecordMapper.selectList(any()))
                .thenReturn(List.of(income, expense))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(investmentTransactionMapper.selectList(any()))
                .thenReturn(List.of(buy))
                .thenReturn(List.of());
        when(adjustmentMapper.selectList(any())).thenReturn(List.of(adjustment));
        when(budgetMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.positionsAt(USER_ID, snapshotDate)).thenReturn(Map.of());

        AssetSnapshotVO snapshot = service.generateForUser(USER_ID, snapshotDate);
        // 历史资产快照现金资产按初始余额 + 当日前资金事件重建，不能直接读取当前 account.balance=9999。
        assertEquals(bd("1075.0000"), snapshot.getCashAsset());
        assertEquals(bd("1075.0000"), snapshot.getTotalAsset());
        assertEquals(bd("1075.0000"), snapshot.getNetAsset());
    }

    @Test
    void assetSnapshotMonthlyFlowShouldOnlyCountMonthToSnapshotDate() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        TransactionRecordMapper transactionRecordMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        BudgetMapper budgetMapper = mock(BudgetMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, accountMapper, adjustmentMapper, mock(AssetMapper.class),
                mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), investmentTransactionMapper,
                transactionRecordMapper, budgetMapper, mock(UserMapper.class), positionHistoryService);
        LocalDate snapshotDate = LocalDate.of(2026, 6, 3);
        TransactionRecord income = record("INCOME", "200.0000", 1L, null, 10L);
        income.setTransactionTime(LocalDateTime.of(2026, 6, 2, 10, 0));
        TransactionRecord expense = record("EXPENSE", "50.0000", 1L, null, 11L);
        expense.setTransactionTime(LocalDateTime.of(2026, 6, 3, 10, 0));

        when(assetSnapshotMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectList(any())).thenReturn(List.of());
        when(adjustmentMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.positionsAt(USER_ID, snapshotDate)).thenReturn(Map.of());
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of());
        when(budgetMapper.selectList(any())).thenReturn(List.of());
        when(transactionRecordMapper.selectList(any()))
                .thenReturn(List.of(income))
                .thenReturn(List.of(expense))
                .thenReturn(List.of())
                .thenReturn(List.of(expense))
                .thenReturn(List.of());

        AssetSnapshotVO snapshot = service.generateForUser(USER_ID, snapshotDate);

        // 补跑历史资产快照时，当月收支只能统计到快照日，不能把快照日之后的流水提前计入。
        assertEquals(bd("200.0000"), snapshot.getMonthlyIncome());
        assertEquals(bd("50.0000"), snapshot.getMonthlyExpense());
        assertEquals(bd("150.0000"), snapshot.getMonthlyBalance());
        ArgumentCaptor<LambdaQueryWrapper<TransactionRecord>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(transactionRecordMapper, atLeastOnce()).selectList(wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getAllValues().stream()
                .flatMap(wrapper -> wrapper.getParamNameValuePairs().values().stream())
                .filter(LocalDateTime.class::isInstance)
                .map(LocalDateTime.class::cast)
                .noneMatch(value -> value.toLocalDate().isAfter(snapshotDate)));
    }

    @Test
    void assetSnapshotGenerateShouldUpdateExistingCurrentUserDate() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        TransactionRecordMapper transactionRecordMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        BudgetMapper budgetMapper = mock(BudgetMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, accountMapper, adjustmentMapper, mock(AssetMapper.class),
                mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), investmentTransactionMapper,
                transactionRecordMapper, budgetMapper, mock(UserMapper.class), positionHistoryService);
        LocalDate snapshotDate = LocalDate.of(2026, 6, 5);
        AssetSnapshot exists = assetSnapshot(99L, USER_ID, snapshotDate, "1000.0000");

        when(assetSnapshotMapper.selectOne(any())).thenReturn(exists);
        when(accountMapper.selectList(any())).thenReturn(List.of());
        when(adjustmentMapper.selectList(any())).thenReturn(List.of());
        when(transactionRecordMapper.selectList(any())).thenReturn(List.of());
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of());
        when(budgetMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.positionsAt(USER_ID, snapshotDate)).thenReturn(Map.of());

        AssetSnapshotVO snapshot = service.generate(snapshotDate);
        // 指定日期手动重建只能 upsert 当前用户当天快照，避免同一天生成重复资产快照。
        assertEquals(99L, snapshot.getId());
        verify(assetSnapshotMapper, never()).insert(any(AssetSnapshot.class));
        verify(assetSnapshotMapper).update(any(), any());
    }

    @Test
    void assetSnapshotGenerateShouldRejectFutureDate() {
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                mock(AssetSnapshotMapper.class), mock(AccountMapper.class), mock(AccountBalanceAdjustmentMapper.class), mock(AssetMapper.class),
                mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), mock(InvestmentTransactionMapper.class),
                mock(TransactionRecordMapper.class), mock(BudgetMapper.class), mock(UserMapper.class), mock(InvestmentPositionHistoryService.class));

        assertThrows(BusinessException.class, () -> service.generate(LocalDate.now().plusDays(1)));
    }

    @Test
    void assetSnapshotShouldKeepConfirmedFundBuyInTransitBeforeConfirmedDate() {
        AssetSnapshotMapper assetSnapshotMapper = mock(AssetSnapshotMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionRecordMapper = mock(TransactionRecordMapper.class);
        BudgetMapper budgetMapper = mock(BudgetMapper.class);
        AccountBalanceAdjustmentMapper adjustmentMapper = mock(AccountBalanceAdjustmentMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        SnapshotServiceImpl service = new SnapshotServiceImpl(
                assetSnapshotMapper, accountMapper, adjustmentMapper, mock(AssetMapper.class),
                mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), investmentTransactionMapper,
                transactionRecordMapper, budgetMapper, mock(UserMapper.class), positionHistoryService);
        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setUserId(USER_ID);
        transaction.setType("BUY");
        transaction.setInputMode("AMOUNT_NAV");
        transaction.setStatus("CONFIRMED");
        transaction.setTradeAmount(bd("1524.1000"));
        transaction.setTransactionTime(LocalDateTime.of(2026, 6, 4, 9, 30));
        transaction.setConfirmedDate(LocalDate.of(2026, 6, 8));

        LocalDate snapshotDate = LocalDate.of(2026, 6, 5);
        when(assetSnapshotMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectList(any())).thenReturn(List.of());
        when(adjustmentMapper.selectList(any())).thenReturn(List.of());
        when(transactionRecordMapper.selectList(any())).thenReturn(List.of());
        when(budgetMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.positionsAt(USER_ID, snapshotDate)).thenReturn(Map.of());
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of(transaction));

        AssetSnapshotVO snapshot = service.generateForUser(USER_ID, snapshotDate);
        // 已确认交易重放到确认日前时仍应计作在途投资资产，避免现金已扣但投资资产未入导致净资产假跌。
        assertEquals(bd("1524.1000"), snapshot.getInvestmentAsset());
        assertEquals(bd("1524.1000"), snapshot.getInvestmentCost());
    }

    @Test
    void cryptoHoldingShouldKeepTodayProfitUnavailableWhenCurrentPriceIsStale() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "0.6000", "60.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "DOGE", "Dogecoin", "CRYPTO", "USD")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "0.70000000", "USD", LocalDateTime.now().minusDays(1))));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(dailyPrice(10L, LocalDate.now().minusDays(1), "0.65000000", "USD")));

        HoldingVO vo = service.list().get(0);
        // 虚拟货币虽然全天交易，但当前价如果不是今天，也不能用于今日收益。
        assertEquals(false, vo.getTodayPriceAvailable());
        assertEquals(null, vo.getTodayProfit());
        assertEquals("TODAY_PRICE_NOT_AVAILABLE", vo.getPriceStatus());
    }

    @Test
    void fundHoldingShouldUseNavDisplayDateForTodayProfit() {
        LocalDate today = LocalDate.now();
        LocalDate navDate = today.minusDays(3);
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        Asset fund = asset(10L, "FUND-A", "QDII 基金", "FUND", "CNY");
        AssetPriceCurrent current = price(10L, "11.00000000", "CNY", navDate.atTime(15, 0));
        current.setPreviousClose(bd("10.00000000"));
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(fund));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, current));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(dailyPrice(10L, navDate, "11.00000000", "CNY")));
        when(marketCalendarMapper.selectOne(any())).thenReturn(marketCalendar(today, true, "MANUAL"));
        when(marketCalendarMapper.selectList(any())).thenReturn(List.of(marketCalendar(today, true, "MANUAL")));

        HoldingVO vo = service.list().get(0);

        // QDII 净值日期可能早于今天，但如果收益日历映射到今天，持仓列表今日收益也必须同步展示。
        assertEquals(today, vo.getPriceDate().plusDays(3));
        assertEquals(true, vo.getTodayPriceAvailable());
        assertEquals("NORMAL", vo.getPriceStatus());
        assertEquals(bd("100.0000"), vo.getTodayProfit());
    }

    @Test
    void holdingYesterdayProfitShouldBeNullWhenDailyIsMissing() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.now())));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(holding.getQuantity());

        HoldingVO vo = service.list().get(0);
        assertEquals(null, vo.getYesterdayProfit());
        assertEquals(null, vo.getYesterdayChangeRate());
    }

    @Test
    void holdingYesterdayProfitShouldUseDailyEvenWhenBackfilledFlat() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.now())));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(
                dailyPrice(10L, LocalDate.now().minusDays(1), "11.00000000", "CNY"),
                dailyPrice(10L, LocalDate.now().minusDays(2), "11.00000000", "CNY")));
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(holding.getQuantity());

        HoldingVO vo = service.list().get(0);
        assertEquals(bd("0.0000"), vo.getYesterdayProfit());
        assertEquals(bd("0.0000"), vo.getYesterdayChangeRate());
    }

    @Test
    void fundProfitCalendarShouldMarkWeekendAsMarketClosed() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of());

        List<InvestmentCalendarDayProfitVO> calendar = service.profitCalendar(1L, YearMonth.of(2026, 5));
        InvestmentCalendarDayProfitVO weekend = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 2).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(false, weekend.getTradingDay());
        assertEquals(true, weekend.getMarketClosed());
        assertEquals("休市", weekend.getStatusLabel());
    }

    @Test
    void stockProfitCalendarShouldUseCurrentPriceWhenDailyIsStaleForQuoteDate() {
        LocalDate today = LocalDate.now();
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        Asset stock = asset(10L, "600666.SH", "奥瑞德", "STOCK", "CNY");
        stock.setMarket("SH");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, currentMapper, dailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, mock(QuoteService.class));
        AssetPriceCurrent current = price(10L, "12.00000000", "CNY", today.atTime(15, 0));
        current.setPreviousClose(bd("10.00000000"));

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(stock);
        when(currentMapper.selectById(10L)).thenReturn(current);
        when(dailyMapper.selectList(any())).thenReturn(List.of(
                dailyPrice(10L, today.minusDays(1), "10.00000000", "CNY"),
                dailyPrice(10L, today, "11.00000000", "CNY")));
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(holding.getQuantity());

        InvestmentCalendarDayProfitVO todayCell = service.profitCalendar(1L, YearMonth.from(today)).stream()
                .filter(item -> today.equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        // 股票 current 可能先于 daily 聚合更新；日历要展示最新 current，不能继续用旧 daily。
        assertEquals(bd("12.00000000"), todayCell.getPrice());
        assertEquals(bd("200.0000"), todayCell.getProfitAmount());
    }

    @Test
    void stockProfitCalendarShouldKeepDailyWhenCurrentPriceIsMissing() {
        LocalDate today = LocalDate.now();
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        Asset stock = asset(10L, "600666.SH", "奥瑞德", "STOCK", "CNY");
        stock.setMarket("SH");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, currentMapper, dailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, mock(QuoteService.class));
        AssetPriceCurrent current = price(10L, "12.00000000", "CNY", today.atTime(15, 0));
        current.setPrice(null);

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(stock);
        when(currentMapper.selectById(10L)).thenReturn(current);
        when(dailyMapper.selectList(any())).thenReturn(List.of(
                dailyPrice(10L, today.minusDays(1), "10.00000000", "CNY"),
                dailyPrice(10L, today, "11.00000000", "CNY")));
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(holding.getQuantity());

        InvestmentCalendarDayProfitVO todayCell = service.profitCalendar(1L, YearMonth.from(today)).stream()
                .filter(item -> today.equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        // current 只有报价时间但价格为空时不能覆盖有效 daily。
        assertEquals(bd("11.00000000"), todayCell.getPrice());
        assertEquals(bd("100.0000"), todayCell.getProfitAmount());
    }

    @Test
    void stockProfitCalendarShouldKeepDailyPreviousCloseWhenCurrentPreviousCloseIsMissing() {
        LocalDate today = LocalDate.now();
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        Asset stock = asset(10L, "600666.SH", "奥瑞德", "STOCK", "CNY");
        stock.setMarket("SH");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, currentMapper, dailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, mock(QuoteService.class));
        AssetPriceDaily daily = dailyPrice(10L, today, "11.00000000", "CNY");
        daily.setPreviousClose(bd("10.00000000"));
        AssetPriceCurrent current = price(10L, "12.00000000", "CNY", today.atTime(15, 0));
        current.setPreviousClose(null);

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(stock);
        when(currentMapper.selectById(10L)).thenReturn(current);
        when(dailyMapper.selectList(any())).thenReturn(List.of(daily));
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(holding.getQuantity());

        InvestmentCalendarDayProfitVO todayCell = service.profitCalendar(1L, YearMonth.from(today)).stream()
                .filter(item -> today.equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        // current 覆盖价格时要继承同日 daily 的 previousClose，否则单日价格点收益会算不出来。
        assertEquals(bd("12.00000000"), todayCell.getPrice());
        assertEquals(bd("200.0000"), todayCell.getProfitAmount());
    }

    @Test
    void stockHoldingCreateShouldNotPersistLookupPriceAsDailyClose() {
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        AssetService assetService = mock(AssetService.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, currentMapper, dailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), assetService, mock(InvestmentPositionHistoryService.class), quoteService);
        Asset stock = asset(10L, "600666.SH", "奥瑞德", "STOCK", "CNY");
        stock.setMarket("SH");
        HoldingRequest request = new HoldingRequest();
        request.setAssetId(10L);
        request.setAssetType("STOCK");
        request.setQuantity(bd("600.0000"));
        request.setAvgCost(bd("5.0680"));
        request.setLatestPrice(bd("5.07000000"));
        request.setPreviousClose(bd("4.61000000"));
        request.setQuoteTime(LocalDateTime.of(2026, 6, 3, 9, 32));
        request.setQuoteSource("SINA");

        when(assetService.findAsset(10L)).thenReturn(stock);
        when(holdingMapper.selectCount(any())).thenReturn(0L);
        when(assetMapper.selectById(10L)).thenReturn(stock);
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "5.07000000", "CNY", LocalDateTime.of(2026, 6, 3, 9, 32))));

        service.create(request);

        verify(currentMapper).insert(any(AssetPriceCurrent.class));
        // 股票识别价不能写入日级表，否则盘中或延迟价会污染收益日历收盘价。
        verify(dailyMapper, never()).insert(any(AssetPriceDaily.class));
        verify(dailyMapper, never()).update(any(AssetPriceDaily.class), any());
    }

    @Test
    void stockModuleTrendShouldUseCurrentPriceWhenDailyIsStaleForQuoteDate() {
        LocalDate today = LocalDate.now();
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        Asset stock = asset(10L, "600666.SH", "奥瑞德", "STOCK", "CNY");
        stock.setMarket("SH");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, currentMapper, dailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, mock(QuoteService.class));

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(any())).thenReturn(List.of(stock));
        when(dailyMapper.selectList(any())).thenReturn(List.of(dailyPrice(10L, today, "11.00000000", "CNY")));
        when(currentMapper.selectBatchIds(any())).thenReturn(List.of(price(10L, "12.00000000", "CNY", today.atTime(15, 0))));
        when(positionHistoryService.positionsAt(USER_ID, today)).thenReturn(Map.of(
                1L, new InvestmentPositionState(1L, 10L, bd("100.0000"), bd("1000.0000"))));

        InvestmentTrendVO trend = service.trend("STOCK", "MONTH", today, today);
        // 模块趋势和收益日历同口径合并 current，避免趋势图今日点继续展示旧 daily。
        assertEquals(bd("1200.0000"), trend.getPoints().get(0).getMarketValue());
        assertEquals(bd("200.0000"), trend.getPoints().get(0).getTotalProfit());
    }

    @Test
    void fundProfitCalendarShouldMarkExchangeHolidayAsMarketClosed() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));
        MarketCalendar holiday = new MarketCalendar();
        holiday.setMarket("A_SHARE");
        holiday.setTradeDate(LocalDate.of(2026, 5, 4));
        holiday.setTradingDay(false);
        holiday.setSource("EXCHANGE_ANNOUNCEMENT");

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of());
        // 交易所公告休市日即使落在工作日，基金收益日历也要明确展示休市。
        when(marketCalendarMapper.selectOne(any())).thenReturn(holiday);

        List<InvestmentCalendarDayProfitVO> calendar = service.profitCalendar(1L, YearMonth.of(2026, 5));
        InvestmentCalendarDayProfitVO holidayCell = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 4).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(false, holidayCell.getTradingDay());
        assertEquals(true, holidayCell.getMarketClosed());
        assertEquals("休市", holidayCell.getStatusLabel());
    }

    @Test
    void aShareStockProfitCalendarShouldMarkExchangeHolidayAsMarketClosed() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));
        Asset stock = asset(10L, "600519", "贵州茅台", "STOCK", "CNY");
        stock.setMarket("SH");
        MarketCalendar holiday = new MarketCalendar();
        holiday.setMarket("A_SHARE");
        holiday.setTradeDate(LocalDate.of(2026, 5, 4));
        holiday.setTradingDay(false);
        holiday.setSource("EXCHANGE_ANNOUNCEMENT");

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(stock);
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of());
        // A 股休市日要和基金一样在收益日历里显式展示，不能只显示无价格。
        when(marketCalendarMapper.selectOne(any())).thenReturn(holiday);

        List<InvestmentCalendarDayProfitVO> calendar = service.profitCalendar(1L, YearMonth.of(2026, 5));
        InvestmentCalendarDayProfitVO holidayCell = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 4).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(false, holidayCell.getTradingDay());
        assertEquals(true, holidayCell.getMarketClosed());
        assertEquals("休市", holidayCell.getStatusLabel());
    }

    @Test
    void stockProfitCalendarShouldSuppressPriceOnMarketClosedDay() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, mock(QuoteService.class));
        Asset stock = asset(10L, "600519", "贵州茅台", "STOCK", "CNY");
        stock.setMarket("SH");
        MarketCalendar holiday = marketCalendar(LocalDate.of(2026, 5, 4), false, "EXCHANGE_ANNOUNCEMENT");

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(stock);
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(
                dailyPrice(10L, LocalDate.of(2026, 5, 1), "10.00000000", "CNY"),
                dailyPrice(10L, LocalDate.of(2026, 5, 4), "12.00000000", "CNY")));
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(holding.getQuantity());
        when(marketCalendarMapper.selectOne(any())).thenReturn(holiday);

        InvestmentCalendarDayProfitVO holidayCell = service.profitCalendar(1L, YearMonth.of(2026, 5)).stream()
                .filter(item -> LocalDate.of(2026, 5, 4).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        // 即使脏价格落到休市日，收益日历也只展示休市，不展示误导性的价格/盈亏。
        assertEquals("休市", holidayCell.getStatusLabel());
        assertEquals(false, holidayCell.getHasPrice());
        assertEquals(null, holidayCell.getPrice());
        assertEquals(null, holidayCell.getProfitAmount());
    }

    @Test
    void hkStockProfitCalendarShouldNotUseAShareHoliday() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));
        Asset stock = asset(10L, "00700", "腾讯控股", "STOCK", "HKD");
        stock.setMarket("HK");
        MarketCalendar aShareHoliday = new MarketCalendar();
        aShareHoliday.setMarket("A_SHARE");
        aShareHoliday.setTradeDate(LocalDate.of(2026, 5, 4));
        aShareHoliday.setTradingDay(false);
        aShareHoliday.setSource("EXCHANGE_ANNOUNCEMENT");

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(stock);
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of());
        // 跨市场股票不能误用 A 股节假日；只有 market=A_SHARE 查询才返回这条休市修正。
        when(marketCalendarMapper.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<MarketCalendar> wrapper = invocation.getArgument(0);
            return wrapper.getParamNameValuePairs().containsValue("A_SHARE") ? aShareHoliday : null;
        });

        List<InvestmentCalendarDayProfitVO> calendar = service.profitCalendar(1L, YearMonth.of(2026, 5));
        InvestmentCalendarDayProfitVO holidayCell = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 4).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(true, holidayCell.getTradingDay());
        assertEquals(false, holidayCell.getMarketClosed());
        assertEquals("无价格", holidayCell.getStatusLabel());
    }

    @Test
    void fundProfitCalendarShouldSkipHolidayWhenMappingNavToNextTradingDay() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, marketCalendarMapper, mock(AccountMapper.class), mock(AssetService.class), positionHistoryService, mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(
                dailyPrice(10L, LocalDate.of(2026, 4, 30), "10.00000000", "CNY"),
                dailyPrice(10L, LocalDate.of(2026, 5, 1), "11.00000000", "CNY")));
        when(transactionMapper.selectCount(any())).thenReturn(0L);
        when(positionHistoryService.quantityAt(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(marketCalendarMapper.selectList(any())).thenReturn(List.of(
                marketCalendar(LocalDate.of(2026, 5, 4), false, "EXCHANGE_ANNOUNCEMENT"),
                marketCalendar(LocalDate.of(2026, 5, 4), true, "SYSTEM_WEEKDAY"),
                marketCalendar(LocalDate.of(2026, 5, 5), false, "EXCHANGE_ANNOUNCEMENT"),
                marketCalendar(LocalDate.of(2026, 5, 6), true, "SYSTEM_WEEKDAY")));

        List<InvestmentCalendarDayProfitVO> calendar = service.profitCalendar(1L, YearMonth.of(2026, 5));
        InvestmentCalendarDayProfitVO holidayCell = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 4).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        InvestmentCalendarDayProfitVO tradingCell = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 6).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(false, holidayCell.getHasPrice());
        assertEquals(true, tradingCell.getHasPrice());
        assertEquals(bd("100.0000"), tradingCell.getProfitAmount());
    }

    @Test
    void cryptoProfitCalendarShouldNotMarkWeekendAsMarketClosed() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "DOGE", "Dogecoin", "CRYPTO", "USD"));

        List<InvestmentCalendarDayProfitVO> calendar = service.profitCalendar(1L, YearMonth.of(2026, 5));
        InvestmentCalendarDayProfitVO weekend = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 2).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(true, weekend.getTradingDay());
        assertEquals(false, weekend.getMarketClosed());
    }

    @Test
    void usStockProfitCalendarShouldNotUseAShareHoliday() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        Asset usStock = asset(10L, "AAPL", "Apple", "STOCK", "USD");
        usStock.setMarket("US");
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(InvestmentPositionHistoryService.class), mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(usStock);

        List<InvestmentCalendarDayProfitVO> calendar = service.profitCalendar(1L, YearMonth.of(2026, 5));
        InvestmentCalendarDayProfitVO weekday = calendar.stream()
                .filter(item -> LocalDate.of(2026, 5, 4).equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(true, weekday.getTradingDay());
        assertEquals(false, weekday.getMarketClosed());
    }

    @Test
    void holdingDetailShouldKeepRevokedTransactionsButExcludeThemFromSummary() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), accountMapper, mock(AssetService.class), mock(com.xoassets.module.investment.service.InvestmentPositionHistoryService.class), quoteService);
        InvestmentTransaction buy = investmentRecord(1L, "BUY", "100.0000", "10.0000", "1000.0000", "2.0000", "1002.0000");
        InvestmentTransaction sell = investmentRecord(2L, "SELL", "20.0000", "12.0000", "240.0000", "1.0000", "200.0000");
        sell.setRealizedProfit(bd("39.0000"));
        InvestmentTransaction revoked = investmentRecord(3L, "BUY", "10.0000", "8.0000", "80.0000", "1.0000", "81.0000");
        revoked.setStatus("REVOKED");

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.of(2026, 5, 31, 9, 0))));
        when(transactionMapper.selectList(any())).thenReturn(List.of(sell, revoked, buy));
        when(accountMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(account(1L, USER_ID, "银行卡", "BANK", "1000.0000")));

        HoldingDetailVO detail = service.detail(1L);
        assertEquals(3, detail.getTransactions().size());
        assertEquals(1, detail.getSummary().getBuyCount());
        assertEquals(1, detail.getSummary().getSellCount());
        assertEquals(bd("1002.0000"), detail.getSummary().getTotalBuyAmount());
        assertEquals(bd("239.0000"), detail.getSummary().getTotalSellAmount());
        assertEquals(bd("39.0000"), detail.getSummary().getRealizedProfit());
        assertEquals(bd("100.0000"), detail.getSummary().getFloatingProfit());
        assertEquals(bd("139.0000"), detail.getSummary().getTotalProfit());
    }

    @Test
    void holdingDetailShouldHandleMissingPriceSnapshots() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), mock(AccountMapper.class), mock(AssetService.class), mock(com.xoassets.module.investment.service.InvestmentPositionHistoryService.class), mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(transactionMapper.selectList(any())).thenReturn(List.of());

        HoldingDetailVO detail = service.detail(1L);
        assertEquals(0, detail.getPriceSnapshots().size());
        assertEquals(bd("10.0000"), detail.getHolding().getLatestPrice());
        assertEquals(bd("1000.0000"), detail.getHolding().getMarketValue());
        assertEquals(bd("0.0000"), detail.getSummary().getTotalProfit());
    }

    @Test
    void holdingDetailChartShouldUseHistoricalPositionQuantity() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(MarketCalendarMapper.class), accountMapper, mock(AssetService.class), positionHistoryService, mock(QuoteService.class));

        Asset asset = asset(10L, "FUND-A", "基金 A", "FUND", "CNY");
        AssetPriceDaily first = dailyPrice(10L, LocalDate.of(2026, 5, 1), "10.00000000", "CNY");
        AssetPriceDaily second = dailyPrice(10L, LocalDate.of(2026, 5, 2), "12.00000000", "CNY");
        InvestmentTransaction buy = investmentRecord(1L, "BUY", "100.0000", "10.0000", "1000.0000", "0.0000", "1000.0000");

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset);
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(second, first));
        when(transactionMapper.selectList(any())).thenReturn(List.of(buy));
        when(accountMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(account(1L, USER_ID, "银行卡", "BANK", "1000.0000")));
        when(positionHistoryService.positionsAt(USER_ID, LocalDate.of(2026, 5, 1)))
                .thenReturn(Map.of(1L, new InvestmentPositionState(1L, 10L, bd("50.0000000000"), bd("500.0000"))));
        when(positionHistoryService.positionsAt(USER_ID, LocalDate.of(2026, 5, 2)))
                .thenReturn(Map.of(1L, new InvestmentPositionState(1L, 10L, bd("100.0000000000"), bd("1000.0000"))));

        HoldingDetailVO detail = service.detail(1L);
        // 详情趋势图按每个价格日期的历史份额计算市值，避免加仓后把 5 月 1 日也画成当前 100 份。
        assertEquals(bd("500.0000"), detail.getChartPoints().get(0).getTotalAssetAmount());
        assertEquals(bd("1200.0000"), detail.getChartPoints().get(1).getTotalAssetAmount());
        assertEquals(bd("200.0000"), detail.getChartPoints().get(1).getTotalProfitAmount());
    }

    @Test
    void budgetSummaryShouldSubtractRefundAndIgnoreTransfer() {
        BudgetMapper budgetMapper = mock(BudgetMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        BudgetServiceImpl service = new BudgetServiceImpl(budgetMapper, categoryMapper, transactionMapper, mock(CategoryService.class));
        Budget totalBudget = budget(1L, null, "TOTAL", "1000.0000");
        Budget foodBudget = budget(2L, 10L, "CATEGORY", "300.0000");

        when(budgetMapper.selectList(any())).thenReturn(List.of(totalBudget, foodBudget));
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(10L, USER_ID, "餐饮", "EXPENSE")));
        when(transactionMapper.selectList(any()))
                .thenReturn(List.of(record("EXPENSE", "120.0000", 1L, null, 10L), record("REFUND", "20.0000", 1L, null, 10L)))
                .thenReturn(List.of(record("EXPENSE", "120.0000", 1L, null, 10L), record("REFUND", "20.0000", 1L, null, 10L)))
                .thenReturn(List.of(record("EXPENSE", "120.0000", 1L, null, 10L), record("REFUND", "20.0000", 1L, null, 10L)));

        BudgetSummaryVO summary = service.summary("2026-05");
        assertEquals(bd("100.0000"), summary.getTotalUsed());
        assertEquals(bd("10.0000"), summary.getUsageRate());
        assertEquals(bd("100.0000"), summary.getItems().get(1).getUsedAmount());
        assertEquals(bd("33.3333"), summary.getItems().get(1).getUsageRate());
    }

    @Test
    void dashboardAndStatisticsShouldUseAccountsPlusInvestmentMarketValue() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        HoldingService holdingService = mock(HoldingService.class);
        com.xoassets.module.budget.service.BudgetService budgetService = mock(com.xoassets.module.budget.service.BudgetService.class);
        DashboardServiceImpl dashboard = new DashboardServiceImpl(
                accountMapper, transactionMapper, investmentTransactionMapper, holdingService, budgetService);

        HoldingVO holding = HoldingVO.builder()
                .assetName("DOGE")
                .assetType("CRYPTO")
                .marketValue(bd("638.3592"))
                .floatingProfit(bd("109.5660"))
                .build();
        when(accountMapper.selectList(any())).thenReturn(List.of(account(1L, USER_ID, "银行卡", "BANK", "1000.0000")));
        when(holdingService.list()).thenReturn(List.of(holding));
        // 首页今日盈亏直接复用投资总览，避免和投资页出现两套今日收益口径。
        when(holdingService.overview()).thenReturn(InvestmentOverviewVO.builder()
                .totalInvestmentAsset(bd("638.3592"))
                .holdingProfit(bd("109.5660"))
                .todayProfit(bd("12.3400"))
                .build());
        // 首页投资总收益需要把已实现卖出收益和当前持仓浮动收益合并展示。
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of(investmentTransaction("SELL", "20.0000", "NORMAL")));
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(budgetService.summary("2026-05")).thenReturn(BudgetSummaryVO.builder().usageRate(bd("10.0000")).items(List.of()).build());

        DashboardOverviewVO overview = dashboard.overview(YearMonth.of(2026, 5));
        assertEquals(bd("1638.3592"), overview.getTotalAssets());
        assertEquals(bd("129.5660"), overview.getInvestmentTotalProfit());
        assertEquals(bd("12.3400"), overview.getInvestmentTodayProfit());

        StatisticsServiceImpl statistics = new StatisticsServiceImpl(
                accountMapper, mock(AssetSnapshotMapper.class), mock(CategoryMapper.class), transactionMapper, dashboard, holdingService, budgetService);
        List<AssetDistributionVO> distribution = statistics.assetDistribution();
        assertEquals(2, distribution.size());
        assertEquals(bd("61.0367"), distribution.get(0).getPercent());
        assertEquals(bd("38.9633"), distribution.get(1).getPercent());
    }

    @Test
    void dashboardTrendRatesShouldBeNullWhenBaselineIsMissing() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        HoldingService holdingService = mock(HoldingService.class);
        com.xoassets.module.budget.service.BudgetService budgetService = mock(com.xoassets.module.budget.service.BudgetService.class);
        DashboardServiceImpl dashboard = new DashboardServiceImpl(
                accountMapper, transactionMapper, investmentTransactionMapper, holdingService, budgetService);

        when(accountMapper.selectList(any())).thenReturn(List.of(account(1L, USER_ID, "银行卡", "BANK", "1000.0000")));
        when(holdingService.overview()).thenReturn(InvestmentOverviewVO.builder()
                .totalInvestmentAsset(BigDecimal.ZERO)
                .holdingProfit(BigDecimal.ZERO)
                .todayProfit(null)
                .build());
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of());
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(budgetService.summary("2026-05")).thenReturn(BudgetSummaryVO.builder().usageRate(bd("10.0000")).items(List.of()).build());

        DashboardOverviewVO overview = dashboard.overview(YearMonth.of(2026, 5));
        // 首页趋势没有上期基准时返回 null，让 Web 展示 --，不能显示假 0%。
        assertEquals(null, overview.getAssetTrendRate());
        assertEquals(null, overview.getIncomeTrendRate());
        assertEquals(null, overview.getExpenseTrendRate());
        assertEquals(null, overview.getBalanceTrendRate());
    }

    @Test
    void dashboardOverviewShouldReturnTodayAndMonthlyBalanceRatesWithBothDenominators() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        TransactionRecordMapper transactionMapper = mock(TransactionRecordMapper.class);
        InvestmentTransactionMapper investmentTransactionMapper = mock(InvestmentTransactionMapper.class);
        HoldingService holdingService = mock(HoldingService.class);
        com.xoassets.module.budget.service.BudgetService budgetService = mock(com.xoassets.module.budget.service.BudgetService.class);
        DashboardServiceImpl dashboard = new DashboardServiceImpl(
                accountMapper, transactionMapper, investmentTransactionMapper, holdingService, budgetService);
        TransactionRecord monthlyIncome = record("INCOME", "1000.0000", 1L, null, 10L);
        TransactionRecord monthlyExpense = record("EXPENSE", "400.0000", 1L, null, 11L);
        TransactionRecord monthlyRefund = record("REFUND", "100.0000", 1L, null, 11L);
        TransactionRecord todayIncome = record("INCOME", "200.0000", 1L, null, 10L);
        TransactionRecord todayExpense = record("EXPENSE", "50.0000", 1L, null, 11L);

        when(accountMapper.selectList(any())).thenReturn(List.of(account(1L, USER_ID, "银行卡", "BANK", "1000.0000")));
        when(holdingService.overview()).thenReturn(InvestmentOverviewVO.builder()
                .totalInvestmentAsset(BigDecimal.ZERO)
                .holdingProfit(BigDecimal.ZERO)
                .todayProfit(null)
                .build());
        when(investmentTransactionMapper.selectList(any())).thenReturn(List.of());
        when(budgetService.summary("2026-05")).thenReturn(BudgetSummaryVO.builder().usageRate(bd("0.0000")).items(List.of()).build());
        when(transactionMapper.selectList(any()))
                .thenReturn(List.of(monthlyIncome))
                .thenReturn(List.of(monthlyExpense))
                .thenReturn(List.of(monthlyRefund))
                .thenReturn(List.of(todayIncome))
                .thenReturn(List.of(todayExpense))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of());

        DashboardOverviewVO overview = dashboard.overview(YearMonth.of(2026, 5));

        // 首页盈亏率同时返回收入分母和支出分母两个口径，Web 描述同时展示两者。
        assertEquals(bd("700.0000"), overview.getMonthlyBalance());
        assertEquals(bd("70.0000"), overview.getMonthlyBalanceRateByIncome());
        assertEquals(bd("233.3333"), overview.getMonthlyBalanceRateByExpense());
        assertEquals(bd("150.0000"), overview.getTodayBalance());
        assertEquals(bd("75.0000"), overview.getTodayBalanceRateByIncome());
        assertEquals(bd("300.0000"), overview.getTodayBalanceRateByExpense());
    }

    @Test
    void assetPriceDailyAggregateShouldOnlyReadRedisForStockAndCrypto() {
        Holding fundHolding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        Holding stockHolding = holding(2L, USER_ID, 20L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        QuoteRawSnapshotService rawSnapshotService = mock(QuoteRawSnapshotService.class);
        AssetPriceDailyAggregateJob job = new AssetPriceDailyAggregateJob(
                holdingMapper, assetMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), rawSnapshotService);
        LocalDate tradeDate = LocalDate.of(2026, 6, 8);

        when(holdingMapper.selectList(any())).thenReturn(List.of(fundHolding, stockHolding));
        when(assetMapper.selectBatchIds(Set.of(10L, 20L))).thenReturn(List.of(
                asset(10L, "012922", "QDII 基金", "FUND", "CNY"),
                asset(20L, "600666.SH", "奥瑞德", "STOCK", "CNY")));

        job.aggregate(tradeDate);

        // 基金净值直接写 current/daily，不进入 Redis 原始快照聚合，避免历史基金 Redis 残留污染日级价。
        verify(rawSnapshotService, never()).listByDate(10L, tradeDate);
        verify(rawSnapshotService).listByDate(20L, tradeDate);
    }

    @Test
    void fundQuoteRefreshShouldWriteDailyPriceButNotRedisRawSnapshot() {
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        QuoteRawSnapshotService rawSnapshotService = mock(QuoteRawSnapshotService.class);
        AssetService assetService = mock(AssetService.class);
        QuoteProvider provider = mock(QuoteProvider.class);
        QuoteServiceImpl service = new QuoteServiceImpl(
                currentMapper, dailyMapper, mock(MarketCalendarMapper.class), rawSnapshotService, assetService, List.of(provider));
        Asset fund = asset(10L, "012922", "QDII 基金", "FUND", "CNY");
        LocalDateTime quoteTime = LocalDateTime.of(2026, 6, 5, 21, 30);

        when(assetService.findAsset(10L)).thenReturn(fund);
        when(provider.supports(fund)).thenReturn(true);
        when(provider.fetch(fund)).thenReturn(new QuoteFetchResult(bd("1.23456789"), "CNY", bd("1.20000000"), bd("0.03456789"), bd("2.8800"), "EASTMONEY", quoteTime, "CLOSED", "{}"));
        when(currentMapper.selectById(10L)).thenReturn(null);
        when(dailyMapper.selectOne(any())).thenReturn(null);

        service.refreshQuote(10L);

        ArgumentCaptor<AssetPriceDaily> dailyCaptor = ArgumentCaptor.forClass(AssetPriceDaily.class);
        verify(dailyMapper).insert(dailyCaptor.capture());
        AssetPriceDaily daily = dailyCaptor.getValue();
        // 基金净值直接沉淀到日级价格表，不进入 Redis 原始快照层。
        assertEquals(LocalDate.of(2026, 6, 5), daily.getTradeDate());
        assertEquals(bd("1.23456789"), daily.getClosePrice());
        verify(rawSnapshotService, never()).append(any());
    }

    @Test
    void stockQuoteRefreshIfStaleShouldReuseLatestPriceOnClosedTradingDay() {
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        MarketCalendarMapper marketCalendarMapper = mock(MarketCalendarMapper.class);
        AssetService assetService = mock(AssetService.class);
        QuoteProvider provider = mock(QuoteProvider.class);
        QuoteServiceImpl service = new QuoteServiceImpl(
                currentMapper, mock(AssetPriceDailyMapper.class), marketCalendarMapper, mock(QuoteRawSnapshotService.class), assetService, List.of(provider));
        Asset stock = asset(20L, "600666", "奥瑞德", "STOCK", "CNY");
        stock.setMarket("SH");
        AssetPriceCurrent latest = price(20L, "12.34000000", "CNY", LocalDateTime.now().minusDays(1));
        MarketCalendar closed = new MarketCalendar();
        closed.setMarket("A_SHARE");
        closed.setTradeDate(LocalDate.now());
        closed.setTradingDay(false);
        closed.setSource("EXCHANGE_ANNOUNCEMENT");

        when(assetService.findAsset(20L)).thenReturn(stock);
        when(currentMapper.selectById(20L)).thenReturn(latest);
        when(marketCalendarMapper.selectOne(any())).thenReturn(closed);

        AssetPriceVO result = service.refreshQuoteIfStale(20L);

        // 股票休市日定时刷新只复用最近 current，不调用第三方，也不会写 Redis 原始快照。
        assertEquals(bd("12.34000000"), result.getPrice());
        verify(provider, never()).fetch(any());
    }

    @Test
    void redisRawSnapshotShouldReadByDateScoreRange() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RedisQuoteRawSnapshotService service = new RedisQuoteRawSnapshotService(redisTemplate, objectMapper);
        LocalDate date = LocalDate.of(2026, 6, 5);
        QuoteRawSnapshot expected = new QuoteRawSnapshot(10L, bd("1.2300"), "CNY", null, null, null, "SINA", date.atTime(10, 0), "OPEN");
        QuoteRawSnapshot otherAsset = new QuoteRawSnapshot(20L, bd("9.9900"), "CNY", null, null, null, "SINA", date.atTime(10, 5), "OPEN");
        double startScore = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double endScore = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScore(eq("price:snapshot:10:202606"), eq(startScore), eq(endScore)))
                .thenReturn(Set.of(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(otherAsset)));

        List<QuoteRawSnapshot> result = service.listByDate(10L, date);
        // Redis 日级汇总读取必须直接按当天 score 范围取数，不能把整月 ZSET 全量取出再内存筛选。
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).assetId());
        assertEquals(bd("1.2300"), result.get(0).price());
        verify(zSetOperations).rangeByScore(eq("price:snapshot:10:202606"), eq(startScore), eq(endScore));
    }

    @Test
    void investmentSnapshotBackfillShouldUseDailyPriceEvenWhenPriceWasInsertedLater() {
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentDailySnapshotMapper dailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        InvestmentDailySnapshotJob job = new InvestmentDailySnapshotJob(
                positionHistoryService, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper, transactionMapper, dailySnapshotMapper);
        AssetPriceDaily backfilledDaily = dailyPrice(10L, LocalDate.of(2026, 6, 5), "12.00000000", "CNY");
        backfilledDaily.setCreatedAt(LocalDateTime.of(2026, 6, 8, 12, 0));

        when(positionHistoryService.snapshotUserIds(LocalDate.of(2026, 6, 5), LocalDate.now())).thenReturn(Set.of(USER_ID));
        when(positionHistoryService.positionsAt(USER_ID, LocalDate.of(2026, 6, 5)))
                .thenReturn(Map.of(10L, new InvestmentPositionState(1L, 10L, bd("100.0000"), bd("1000.0000"))));
        when(assetPriceDailyMapper.selectOne(any())).thenReturn(backfilledDaily);
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.netInflow(USER_ID, LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 5))).thenReturn(BigDecimal.ZERO);
        when(dailySnapshotMapper.selectOne(any())).thenReturn(null);

        job.snapshot(LocalDate.of(2026, 6, 5));

        ArgumentCaptor<InvestmentDailySnapshot> captor = ArgumentCaptor.forClass(InvestmentDailySnapshot.class);
        verify(dailySnapshotMapper).insert(captor.capture());
        // 周一补跑上周五时，日级价格 created_at 可能是周一；快照必须按 trade_date 使用它修正历史市值。
        assertEquals(bd("1200.0000"), captor.getValue().getMarketValue());
        assertEquals(bd("200.0000"), captor.getValue().getFloatingProfit());
    }

    @Test
    void investmentSnapshotShouldUseCurrentPriceWhenDailyIsStaleForQuoteDate() {
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentDailySnapshotMapper dailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        InvestmentDailySnapshotJob job = new InvestmentDailySnapshotJob(
                positionHistoryService, currentMapper, dailyMapper, transactionMapper, dailySnapshotMapper);
        LocalDate snapshotDate = LocalDate.now();

        when(positionHistoryService.positionsAt(USER_ID, snapshotDate))
                .thenReturn(Map.of(10L, new InvestmentPositionState(1L, 10L, bd("100.0000"), bd("1000.0000"))));
        when(dailyMapper.selectOne(any())).thenReturn(dailyPrice(10L, snapshotDate, "11.00000000", "CNY"));
        when(currentMapper.selectById(10L)).thenReturn(price(10L, "12.00000000", "CNY", snapshotDate.atTime(15, 0)));
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.netInflow(USER_ID, snapshotDate, snapshotDate)).thenReturn(BigDecimal.ZERO);
        when(dailySnapshotMapper.selectOne(any())).thenReturn(null);

        job.snapshotForUser(USER_ID, snapshotDate);

        ArgumentCaptor<InvestmentDailySnapshot> captor = ArgumentCaptor.forClass(InvestmentDailySnapshot.class);
        verify(dailySnapshotMapper).insert(captor.capture());
        // 投资日快照和收益日历同源兜底，避免当天 daily 滞后时继续用旧价计算市值。
        assertEquals(bd("1200.0000"), captor.getValue().getMarketValue());
        assertEquals(bd("200.0000"), captor.getValue().getFloatingProfit());
    }

    @Test
    void investmentSnapshotShouldKeepDailyWhenCurrentPriceIsMissing() {
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        AssetPriceCurrentMapper currentMapper = mock(AssetPriceCurrentMapper.class);
        AssetPriceDailyMapper dailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentDailySnapshotMapper dailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        InvestmentDailySnapshotJob job = new InvestmentDailySnapshotJob(
                positionHistoryService, currentMapper, dailyMapper, transactionMapper, dailySnapshotMapper);
        LocalDate snapshotDate = LocalDate.now();
        AssetPriceCurrent current = price(10L, "12.00000000", "CNY", snapshotDate.atTime(15, 0));
        current.setPrice(null);

        when(positionHistoryService.positionsAt(USER_ID, snapshotDate))
                .thenReturn(Map.of(10L, new InvestmentPositionState(1L, 10L, bd("100.0000"), bd("1000.0000"))));
        when(dailyMapper.selectOne(any())).thenReturn(dailyPrice(10L, snapshotDate, "11.00000000", "CNY"));
        when(currentMapper.selectById(10L)).thenReturn(current);
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.netInflow(USER_ID, snapshotDate, snapshotDate)).thenReturn(BigDecimal.ZERO);
        when(dailySnapshotMapper.selectOne(any())).thenReturn(null);

        job.snapshotForUser(USER_ID, snapshotDate);

        ArgumentCaptor<InvestmentDailySnapshot> captor = ArgumentCaptor.forClass(InvestmentDailySnapshot.class);
        verify(dailySnapshotMapper).insert(captor.capture());
        // current 价格为空时不能覆盖有效日线，否则当天投资快照会回退成成本价。
        assertEquals(bd("1100.0000"), captor.getValue().getMarketValue());
        assertEquals(bd("100.0000"), captor.getValue().getFloatingProfit());
    }

    @Test
    void investmentSnapshotForUserShouldUpdateExistingDate() {
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentDailySnapshotMapper dailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        InvestmentDailySnapshotJob job = new InvestmentDailySnapshotJob(
                positionHistoryService, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper, transactionMapper, dailySnapshotMapper);
        LocalDate snapshotDate = LocalDate.of(2026, 6, 5);
        InvestmentDailySnapshot exists = investmentDailySnapshot(USER_ID, snapshotDate, "1000.0000");
        exists.setId(88L);
        when(positionHistoryService.positionsAt(USER_ID, snapshotDate))
                .thenReturn(Map.of(1L, new InvestmentPositionState(1L, 10L, bd("100.0000"), bd("1000.0000"))));
        when(assetPriceDailyMapper.selectOne(any())).thenReturn(dailyPrice(10L, snapshotDate, "11.00000000", "CNY"));
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(positionHistoryService.netInflow(USER_ID, snapshotDate, snapshotDate)).thenReturn(BigDecimal.ZERO);
        when(dailySnapshotMapper.selectOne(any())).thenReturn(exists);

        job.snapshotForUser(USER_ID, snapshotDate);

        // 手动重建当前用户投资日快照必须 upsert 同一天记录，不能插入重复快照。
        verify(dailySnapshotMapper, never()).insert(any(InvestmentDailySnapshot.class));
        verify(dailySnapshotMapper).update(any(InvestmentDailySnapshot.class), any());
    }

    @Test
    void investmentSnapshotForUserShouldRejectFutureDate() {
        InvestmentDailySnapshotJob job = new InvestmentDailySnapshotJob(
                mock(InvestmentPositionHistoryService.class), mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentTransactionMapper.class), mock(InvestmentDailySnapshotMapper.class));

        assertThrows(BusinessException.class, () -> job.snapshotForUser(USER_ID, LocalDate.now().plusDays(1)));
    }

    @Test
    void investmentSnapshotShouldKeepConfirmedFundBuyInTransitBeforeConfirmedDate() {
        InvestmentPositionHistoryService positionHistoryService = mock(InvestmentPositionHistoryService.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        InvestmentDailySnapshotMapper dailySnapshotMapper = mock(InvestmentDailySnapshotMapper.class);
        InvestmentDailySnapshotJob job = new InvestmentDailySnapshotJob(
                positionHistoryService, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class), transactionMapper, dailySnapshotMapper);
        InvestmentTransaction confirmedFundBuy = investmentRecord(1L, "BUY", "0.0000", "0.0000", "1500.0000", "0.0000", "1500.0000");
        confirmedFundBuy.setInputMode("AMOUNT_NAV");
        confirmedFundBuy.setTradeAmount(bd("1500.0000"));
        confirmedFundBuy.setStatus("CONFIRMED");
        confirmedFundBuy.setTransactionTime(LocalDateTime.of(2026, 6, 4, 9, 30));
        confirmedFundBuy.setConfirmedDate(LocalDate.of(2026, 6, 8));

        when(positionHistoryService.snapshotUserIds(LocalDate.of(2026, 6, 5), LocalDate.now())).thenReturn(Set.of(USER_ID));
        when(positionHistoryService.positionsAt(USER_ID, LocalDate.of(2026, 6, 5))).thenReturn(Map.of());
        when(transactionMapper.selectList(any())).thenReturn(List.of(confirmedFundBuy)).thenReturn(List.of());
        when(positionHistoryService.netInflow(USER_ID, LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 5))).thenReturn(BigDecimal.ZERO);
        when(dailySnapshotMapper.selectOne(any())).thenReturn(null);

        job.snapshot(LocalDate.of(2026, 6, 5));

        ArgumentCaptor<InvestmentDailySnapshot> captor = ArgumentCaptor.forClass(InvestmentDailySnapshot.class);
        verify(dailySnapshotMapper).insert(captor.capture());
        // 交易后来已确认，但 6 月 5 日仍处于申购已扣款、份额未确认状态，历史快照必须保留在途资产。
        assertEquals(bd("1500.0000"), captor.getValue().getMarketValue());
        assertEquals(bd("1500.0000"), captor.getValue().getTotalCost());
        assertEquals(bd("1500.0000"), captor.getValue().getNetInflow());
    }

    @Test
    void accountOwnershipShouldRejectOtherUsersData() {
        AccountServiceImpl accountService = new AccountServiceImpl(mock(AccountMapper.class), mock(TransactionRecordMapper.class), mock(com.xoassets.module.account.service.AccountBalanceService.class));
        assertThrows(BusinessException.class, () -> accountService.findOwnedAccount(999L, USER_ID));
    }

    private static TransactionRequest transaction(String type, String amount, Long accountId, Long targetAccountId, Long categoryId) {
        TransactionRequest request = new TransactionRequest();
        request.setType(type);
        request.setAmount(bd(amount));
        request.setAccountId(accountId);
        request.setTargetAccountId(targetAccountId);
        request.setCategoryId(categoryId);
        request.setTransactionTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        return request;
    }

    private static InvestmentTransactionRequest investmentTransaction(String type, Long holdingId, Long assetId, Long accountId, String quantity, String price, String fee) {
        InvestmentTransactionRequest request = new InvestmentTransactionRequest();
        request.setType(type);
        request.setHoldingId(holdingId);
        request.setAssetId(assetId);
        request.setAccountId(accountId);
        request.setQuantity(bd(quantity));
        request.setPrice(bd(price));
        request.setFee(bd(fee));
        request.setTransactionTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        return request;
    }

    private static InvestmentTransaction investmentRecord(Long id, String type, String quantity, String price, String amount, String fee, String costAmount) {
        InvestmentTransaction record = new InvestmentTransaction();
        record.setId(id);
        record.setUserId(USER_ID);
        record.setHoldingId(1L);
        record.setAssetId(10L);
        record.setAccountId(1L);
        record.setType(type);
        record.setQuantity(bd(quantity));
        record.setPrice(bd(price));
        record.setAmount(bd(amount));
        record.setFee(bd(fee));
        record.setCostAmount(bd(costAmount));
        record.setStatus("NORMAL");
        record.setTransactionTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        return record;
    }

    private static InvestmentTransaction investmentTransaction(String type, String realizedProfit, String status) {
        InvestmentTransaction record = investmentRecord(1L, type, "1.0000", "1.0000", "1.0000", "0.0000", "1.0000");
        record.setRealizedProfit(bd(realizedProfit));
        record.setStatus(status);
        return record;
    }

    private static InvestmentTransaction pendingFundBuy(LocalDate confirmedDate) {
        InvestmentTransaction record = investmentRecord(1L, "BUY", "0.0000", "0.0000", "999.0000", "1.0000", "1000.0000");
        record.setInputMode("AMOUNT_NAV");
        record.setTradeAmount(bd("1000.0000"));
        record.setConfirmedDate(confirmedDate);
        record.setStatus("PENDING_CONFIRM");
        record.setTransactionTime(LocalDateTime.of(2026, 6, 4, 9, 30));
        return record;
    }

    private static TransactionRecord record(String type, String amount, Long accountId, Long targetAccountId, Long categoryId) {
        TransactionRecord record = new TransactionRecord();
        record.setUserId(USER_ID);
        record.setType(type);
        record.setAmount(bd(amount));
        record.setAccountId(accountId);
        record.setTargetAccountId(targetAccountId);
        record.setCategoryId(categoryId);
        record.setTransactionTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        return record;
    }

    private static AccountBalanceAdjustment adjustment(Long id, Long accountId, String deltaAmount, LocalDate bizDate) {
        AccountBalanceAdjustment adjustment = new AccountBalanceAdjustment();
        adjustment.setId(id);
        adjustment.setUserId(USER_ID);
        adjustment.setAccountId(accountId);
        adjustment.setBeforeBalance(bd("1000.0000"));
        adjustment.setDeltaAmount(bd(deltaAmount));
        adjustment.setAfterBalance(bd("1000.0000").add(bd(deltaAmount)));
        adjustment.setReason("对账修正");
        adjustment.setBizDate(bizDate);
        return adjustment;
    }

    private static Account account(Long id, Long userId, String name, String type, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUserId(userId);
        account.setName(name);
        account.setType(type);
        account.setBalance(bd(balance));
        account.setInitialBalance(bd(balance));
        account.setStatus(1);
        return account;
    }

    private static Category category(Long id, Long userId, String name, String type) {
        Category category = new Category();
        category.setId(id);
        category.setUserId(userId);
        category.setName(name);
        category.setType(type);
        category.setStatus(1);
        return category;
    }

    private static Holding holding(Long id, Long userId, Long assetId, String quantity, String avgCost, String totalCost) {
        Holding holding = new Holding();
        holding.setId(id);
        holding.setUserId(userId);
        holding.setAssetId(assetId);
        holding.setQuantity(bd(quantity));
        holding.setAvgCost(bd(avgCost));
        holding.setTotalCost(bd(totalCost));
        holding.setStatus(1);
        return holding;
    }

    private static Asset asset(Long id, String symbol, String name, String type, String currency) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setType(type);
        asset.setMarket("CRYPTO".equals(type) ? "CRYPTO" : "FUND".equals(type) ? "CN_FUND" : "UNKNOWN");
        asset.setCurrency(currency);
        asset.setQuoteSource("MANUAL");
        return asset;
    }

    private static AssetPriceCurrent price(Long assetId, String price, String currency) {
        return price(assetId, price, currency, LocalDateTime.of(2026, 5, 31, 9, 0));
    }

    private static AssetPriceCurrent price(Long assetId, String price, String currency, LocalDateTime quoteTime) {
        AssetPriceCurrent assetPrice = new AssetPriceCurrent();
        assetPrice.setAssetId(assetId);
        assetPrice.setPrice(bd(price));
        assetPrice.setCurrency(currency);
        assetPrice.setSource("MANUAL");
        assetPrice.setQuoteTime(quoteTime);
        return assetPrice;
    }

    private static AssetPriceDaily dailyPrice(Long assetId, LocalDate tradeDate, String closePrice, String currency) {
        AssetPriceDaily daily = new AssetPriceDaily();
        daily.setAssetId(assetId);
        daily.setTradeDate(tradeDate);
        daily.setClosePrice(bd(closePrice));
        daily.setCurrency(currency);
        daily.setSource("MANUAL");
        return daily;
    }

    private static MarketCalendar marketCalendar(LocalDate tradeDate, boolean tradingDay, String source) {
        MarketCalendar calendar = new MarketCalendar();
        calendar.setMarket("A_SHARE");
        calendar.setTradeDate(tradeDate);
        calendar.setTradingDay(tradingDay);
        calendar.setSource(source);
        return calendar;
    }

    private static InvestmentDailySnapshot investmentDailySnapshot(Long userId, LocalDate snapshotDate, String marketValue) {
        InvestmentDailySnapshot snapshot = new InvestmentDailySnapshot();
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setMarketValue(bd(marketValue));
        snapshot.setNetInflow(bd("0.0000"));
        return snapshot;
    }

    private static AssetSnapshot assetSnapshot(Long id, Long userId, LocalDate snapshotDate, String netAsset) {
        AssetSnapshot snapshot = new AssetSnapshot();
        snapshot.setId(id);
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setCashAsset(BigDecimal.ZERO);
        snapshot.setInvestmentAsset(BigDecimal.ZERO);
        snapshot.setTotalAsset(BigDecimal.ZERO);
        snapshot.setLiability(BigDecimal.ZERO);
        snapshot.setNetAsset(bd(netAsset));
        snapshot.setInvestmentCost(BigDecimal.ZERO);
        snapshot.setInvestmentProfit(BigDecimal.ZERO);
        snapshot.setInvestmentProfitRate(BigDecimal.ZERO);
        snapshot.setMonthlyIncome(BigDecimal.ZERO);
        snapshot.setMonthlyExpense(BigDecimal.ZERO);
        snapshot.setMonthlyBalance(BigDecimal.ZERO);
        snapshot.setBudgetUsedAmount(BigDecimal.ZERO);
        snapshot.setBudgetTotalAmount(BigDecimal.ZERO);
        snapshot.setBudgetUsageRate(BigDecimal.ZERO);
        return snapshot;
    }

    private static Budget budget(Long id, Long categoryId, String type, String amount) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setUserId(USER_ID);
        budget.setMonth("2026-05");
        budget.setCategoryId(categoryId);
        budget.setBudgetType(type);
        budget.setAmount(bd(amount));
        budget.setStatus(1);
        return budget;
    }

    private static void mockAtomicBalance(AccountMapper accountMapper, Account... accounts) {
        when(accountMapper.incrementBalance(any(), any(), any())).thenAnswer(invocation -> {
            Long accountId = invocation.getArgument(1);
            BigDecimal delta = invocation.getArgument(2);
            for (Account account : accounts) {
                if (account.getId().equals(accountId)) {
                    account.setBalance(account.getBalance().add(delta));
                    return 1;
                }
            }
            return 0;
        });
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
