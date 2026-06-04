package com.xoassets.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xoassets.common.api.PageResult;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUser;
import com.xoassets.module.account.service.impl.AccountServiceImpl;
import com.xoassets.module.account.service.impl.AccountLedgerServiceImpl;
import com.xoassets.module.account.vo.AccountFlowStatisticsVO;
import com.xoassets.module.account.vo.AccountLedgerPageVO;
import com.xoassets.module.budget.service.impl.BudgetServiceImpl;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.category.service.CategoryService;
import com.xoassets.module.dashboard.service.impl.DashboardServiceImpl;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.InvestmentTransactionService;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionRevokeRequest;
import com.xoassets.module.investment.service.impl.HoldingServiceImpl;
import com.xoassets.module.investment.service.impl.InvestmentTransactionServiceImpl;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.HoldingDetailVO;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import com.xoassets.module.statistics.service.impl.StatisticsServiceImpl;
import com.xoassets.module.statistics.vo.AssetDistributionVO;
import com.xoassets.module.transaction.dto.TransactionQuery;
import com.xoassets.module.transaction.dto.TransactionRequest;
import com.xoassets.module.transaction.service.TransactionService;
import com.xoassets.module.transaction.service.impl.TransactionServiceImpl;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.entity.AssetPriceDaily;
import com.xoassets.persistence.entity.Budget;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import com.xoassets.persistence.entity.InvestmentTransaction;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.AssetPriceCurrentMapper;
import com.xoassets.persistence.mapper.AssetPriceDailyMapper;
import com.xoassets.persistence.mapper.AssetPriceMapper;
import com.xoassets.persistence.mapper.AssetSnapshotMapper;
import com.xoassets.persistence.mapper.BudgetMapper;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentDailySnapshotMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
        AccountServiceImpl accountService = new AccountServiceImpl(accountMapper, transactionMapper);
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                transactionMapper, accountMapper, mock(CategoryMapper.class), accountService, categoryService);

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
        AssetPriceMapper assetPriceMapper = mock(AssetPriceMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        AssetService assetService = mock(AssetService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, assetPriceMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(AccountMapper.class), assetService, quoteService);

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
        AssetPrice price = price(10L, "16.00000000", "USD");
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
        AssetPriceMapper assetPriceMapper = mock(AssetPriceMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AssetService assetService = mock(AssetService.class);
        AccountServiceImpl accountService = new AccountServiceImpl(accountMapper, mock(TransactionRecordMapper.class));
        HoldingServiceImpl holdingService = new HoldingServiceImpl(
                holdingMapper, assetMapper, assetPriceMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, accountMapper, assetService, mock(QuoteService.class));
        InvestmentTransactionServiceImpl transactionService = new InvestmentTransactionServiceImpl(
                transactionMapper, assetMapper, accountMapper, assetService, holdingService, accountService);

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
        AccountServiceImpl accountService = new AccountServiceImpl(accountMapper, mock(TransactionRecordMapper.class));
        HoldingServiceImpl holdingService = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(AssetPriceMapper.class), mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, accountMapper, mock(AssetService.class), mock(QuoteService.class));
        InvestmentTransactionServiceImpl transactionService = new InvestmentTransactionServiceImpl(
                transactionMapper, assetMapper, accountMapper, mock(AssetService.class), holdingService, accountService);
        InvestmentTransaction sell = investmentRecord(99L, "SELL", "50.0000", "12.0000", "600.0000", "2.0000", "451.0000");

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
                new AccountServiceImpl(accountMapper, transactionMapper), accountMapper, transactionMapper, investmentMapper, categoryMapper, assetMapper);

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
                new AccountServiceImpl(accountMapper, transactionMapper), accountMapper, transactionMapper, investmentMapper, mock(CategoryMapper.class), assetMapper);
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
    void holdingProfitAnalysisShouldUseLatestAndHistoricalPrices() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceMapper assetPriceMapper = mock(AssetPriceMapper.class);
        AssetPriceDailyMapper assetPriceDailyMapper = mock(AssetPriceDailyMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, assetPriceMapper, mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                mock(InvestmentDailySnapshotMapper.class), mock(InvestmentTransactionMapper.class), mock(AccountMapper.class), mock(AssetService.class), quoteService);
        Asset asset = asset(10L, "FUND-A", "基金 A", "FUND", "CNY");
        AssetPrice latest = price(10L, "11.00000000", "CNY", LocalDateTime.now());
        AssetPriceDaily previous = dailyPrice(10L, LocalDate.now().minusDays(1), "9.00000000", "CNY");
        AssetPriceDaily beforePrevious = dailyPrice(10L, LocalDate.now().minusDays(2), "8.00000000", "CNY");

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, latest));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of(previous, beforePrevious));

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
                holdingMapper, assetMapper, mock(AssetPriceMapper.class), mock(AssetPriceCurrentMapper.class), assetPriceDailyMapper,
                investmentDailySnapshotMapper, mock(InvestmentTransactionMapper.class), mock(AccountMapper.class), mock(AssetService.class), quoteService);

        when(holdingMapper.selectList(any())).thenReturn(List.of(holding));
        when(assetMapper.selectBatchIds(Set.of(10L))).thenReturn(List.of(asset(10L, "FUND-A", "基金 A", "FUND", "CNY")));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.now())));
        when(assetPriceDailyMapper.selectList(any())).thenReturn(List.of());
        when(investmentDailySnapshotMapper.selectList(any()))
                .thenReturn(List.of(investmentDailySnapshot(USER_ID, LocalDate.now().minusDays(1), "1000.0000")))
                .thenReturn(List.of());

        assertEquals(bd("100.0000"), service.summary().getTodayProfit());
    }

    @Test
    void holdingDetailShouldKeepRevokedTransactionsButExcludeThemFromSummary() {
        Holding holding = holding(1L, USER_ID, 10L, "100.0000", "10.0000", "1000.0000");
        HoldingMapper holdingMapper = mock(HoldingMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        AssetPriceMapper assetPriceMapper = mock(AssetPriceMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        QuoteService quoteService = mock(QuoteService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, assetPriceMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, accountMapper, mock(AssetService.class), quoteService);
        InvestmentTransaction buy = investmentRecord(1L, "BUY", "100.0000", "10.0000", "1000.0000", "2.0000", "1002.0000");
        InvestmentTransaction sell = investmentRecord(2L, "SELL", "20.0000", "12.0000", "240.0000", "1.0000", "200.0000");
        sell.setRealizedProfit(bd("39.0000"));
        InvestmentTransaction revoked = investmentRecord(3L, "BUY", "10.0000", "8.0000", "80.0000", "1.0000", "81.0000");
        revoked.setStatus("REVOKED");

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(quoteService.latestPriceMap(Set.of(10L))).thenReturn(Map.of(10L, price(10L, "11.00000000", "CNY", LocalDateTime.of(2026, 5, 31, 9, 0))));
        when(assetPriceMapper.selectList(any())).thenReturn(List.of(price(10L, "11.00000000", "CNY", LocalDateTime.of(2026, 5, 31, 9, 0))));
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
        AssetPriceMapper assetPriceMapper = mock(AssetPriceMapper.class);
        InvestmentTransactionMapper transactionMapper = mock(InvestmentTransactionMapper.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, assetPriceMapper, mock(AssetPriceCurrentMapper.class), mock(AssetPriceDailyMapper.class),
                mock(InvestmentDailySnapshotMapper.class), transactionMapper, mock(AccountMapper.class), mock(AssetService.class), mock(QuoteService.class));

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        when(assetMapper.selectById(10L)).thenReturn(asset(10L, "FUND-A", "基金 A", "FUND", "CNY"));
        when(assetPriceMapper.selectList(any())).thenReturn(List.of());
        when(transactionMapper.selectList(any())).thenReturn(List.of());

        HoldingDetailVO detail = service.detail(1L);
        assertEquals(0, detail.getPriceSnapshots().size());
        assertEquals(bd("10.0000"), detail.getHolding().getLatestPrice());
        assertEquals(bd("1000.0000"), detail.getHolding().getMarketValue());
        assertEquals(bd("0.0000"), detail.getSummary().getTotalProfit());
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
        TransactionService transactionService = mock(TransactionService.class);
        HoldingService holdingService = mock(HoldingService.class);
        InvestmentTransactionService investmentTransactionService = mock(InvestmentTransactionService.class);
        com.xoassets.module.budget.service.BudgetService budgetService = mock(com.xoassets.module.budget.service.BudgetService.class);
        DashboardServiceImpl dashboard = new DashboardServiceImpl(
                accountMapper, transactionMapper, transactionService, holdingService, investmentTransactionService, budgetService);

        HoldingVO holding = HoldingVO.builder()
                .assetName("DOGE")
                .assetType("CRYPTO")
                .marketValue(bd("638.3592"))
                .floatingProfit(bd("109.5660"))
                .build();
        when(accountMapper.selectList(any())).thenReturn(List.of(account(1L, USER_ID, "银行卡", "BANK", "1000.0000")));
        when(holdingService.list()).thenReturn(List.of(holding));
        when(transactionMapper.selectList(any())).thenReturn(List.of());
        when(budgetService.summary("2026-05")).thenReturn(BudgetSummaryVO.builder().usageRate(bd("10.0000")).items(List.of()).build());
        when(transactionService.page(any(TransactionQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 1, 5));
        when(investmentTransactionService.list(null)).thenReturn(List.of());

        assertEquals(bd("1638.3592"), dashboard.overview(YearMonth.of(2026, 5)).getTotalAssets());

        StatisticsServiceImpl statistics = new StatisticsServiceImpl(
                accountMapper, mock(AssetSnapshotMapper.class), mock(CategoryMapper.class), transactionMapper, dashboard, holdingService, budgetService);
        List<AssetDistributionVO> distribution = statistics.assetDistribution();
        assertEquals(2, distribution.size());
        assertEquals(bd("61.0367"), distribution.get(0).getPercent());
        assertEquals(bd("38.9633"), distribution.get(1).getPercent());
    }

    @Test
    void accountOwnershipShouldRejectOtherUsersData() {
        AccountServiceImpl accountService = new AccountServiceImpl(mock(AccountMapper.class), mock(TransactionRecordMapper.class));
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

    private static AssetPrice price(Long assetId, String price, String currency) {
        return price(assetId, price, currency, LocalDateTime.of(2026, 5, 31, 9, 0));
    }

    private static AssetPrice price(Long assetId, String price, String currency, LocalDateTime quoteTime) {
        AssetPrice assetPrice = new AssetPrice();
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

    private static InvestmentDailySnapshot investmentDailySnapshot(Long userId, LocalDate snapshotDate, String marketValue) {
        InvestmentDailySnapshot snapshot = new InvestmentDailySnapshot();
        snapshot.setUserId(userId);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setMarketValue(bd(marketValue));
        snapshot.setNetInflow(bd("0.0000"));
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

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
