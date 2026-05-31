package com.xoassets.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xoassets.common.api.PageResult;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUser;
import com.xoassets.module.account.service.impl.AccountServiceImpl;
import com.xoassets.module.budget.service.impl.BudgetServiceImpl;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.category.service.CategoryService;
import com.xoassets.module.dashboard.service.impl.DashboardServiceImpl;
import com.xoassets.module.investment.service.AssetService;
import com.xoassets.module.investment.service.HoldingService;
import com.xoassets.module.investment.service.InvestmentTransactionService;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.service.impl.HoldingServiceImpl;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.statistics.service.impl.StatisticsServiceImpl;
import com.xoassets.module.statistics.vo.AssetDistributionVO;
import com.xoassets.module.transaction.dto.TransactionQuery;
import com.xoassets.module.transaction.dto.TransactionRequest;
import com.xoassets.module.transaction.service.TransactionService;
import com.xoassets.module.transaction.service.impl.TransactionServiceImpl;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Asset;
import com.xoassets.persistence.entity.AssetPrice;
import com.xoassets.persistence.entity.Budget;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.Holding;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.AssetMapper;
import com.xoassets.persistence.mapper.BudgetMapper;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.HoldingMapper;
import com.xoassets.persistence.mapper.InvestmentTransactionMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
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
        QuoteService quoteService = mock(QuoteService.class);
        AssetService assetService = mock(AssetService.class);
        HoldingServiceImpl service = new HoldingServiceImpl(
                holdingMapper, assetMapper, mock(InvestmentTransactionMapper.class), assetService, quoteService);

        when(holdingMapper.selectOne(any())).thenReturn(holding);
        service.applyBuy(USER_ID, 1L, 10L, bd("10.0000"), bd("20.0000"), bd("0.0000"));
        assertEquals(bd("20.0000"), holding.getQuantity());
        assertEquals(bd("300.0000"), holding.getTotalCost());
        assertEquals(bd("15.0000"), holding.getAvgCost());

        service.applySell(USER_ID, 1L, 10L, bd("5.0000"));
        assertEquals(bd("15.0000"), holding.getQuantity());
        assertEquals(bd("225.0000"), holding.getTotalCost());

        assertThrows(BusinessException.class, () -> service.applySell(USER_ID, 1L, 10L, bd("20.0000")));

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
                accountMapper, mock(CategoryMapper.class), transactionMapper, dashboard, holdingService, budgetService);
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
        asset.setCurrency(currency);
        asset.setQuoteSource("MANUAL");
        return asset;
    }

    private static AssetPrice price(Long assetId, String price, String currency) {
        AssetPrice assetPrice = new AssetPrice();
        assetPrice.setAssetId(assetId);
        assetPrice.setPrice(bd(price));
        assetPrice.setCurrency(currency);
        assetPrice.setSource("MANUAL");
        assetPrice.setQuoteTime(LocalDateTime.of(2026, 5, 31, 9, 0));
        return assetPrice;
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
