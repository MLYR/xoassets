package com.xoassets.module.budget.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.budget.dto.BudgetRequest;
import com.xoassets.module.budget.service.BudgetService;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.budget.vo.BudgetVO;
import com.xoassets.module.category.service.CategoryService;
import com.xoassets.persistence.entity.Budget;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.BudgetMapper;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预算服务实现：预算归属当前用户，使用额从流水实时汇总。
 */
@Service
public class BudgetServiceImpl implements BudgetService {

    private static final String TYPE_TOTAL = "TOTAL";
    private static final String TYPE_CATEGORY = "CATEGORY";
    private static final String TRANSACTION_EXPENSE = "EXPENSE";
    private static final String TRANSACTION_REFUND = "REFUND";

    private final BudgetMapper budgetMapper;
    private final CategoryMapper categoryMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final CategoryService categoryService;

    public BudgetServiceImpl(
            BudgetMapper budgetMapper,
            CategoryMapper categoryMapper,
            TransactionRecordMapper transactionRecordMapper,
            CategoryService categoryService) {
        this.budgetMapper = budgetMapper;
        this.categoryMapper = categoryMapper;
        this.transactionRecordMapper = transactionRecordMapper;
        this.categoryService = categoryService;
    }

    /**
     * 查询当前用户某月预算，附带使用金额和进度状态。
     */
    @Override
    public List<BudgetVO> list(String month) {
        Long userId = LoginUserContext.getUserId();
        YearMonth yearMonth = parseMonth(month);
        List<Budget> budgets = budgetMapper.selectList(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, yearMonth.toString())
                .orderByAsc(Budget::getBudgetType)
                .orderByDesc(Budget::getCreatedAt));
        return toVOList(userId, yearMonth, budgets);
    }

    /**
     * 新建预算前校验月度唯一性，避免重复总预算或重复分类预算。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BudgetVO create(BudgetRequest request) {
        Long userId = LoginUserContext.getUserId();
        YearMonth month = parseMonth(request.getMonth());
        validateRequest(userId, request, null);
        ensureUnique(userId, month.toString(), request.getBudgetType(), request.getCategoryId(), null);
        Budget budget = toEntity(userId, month.toString(), request);
        budgetMapper.insert(budget);
        return toVOList(userId, month, List.of(budget)).get(0);
    }

    /**
     * 修改当前用户自己的预算，按 user_id 限定更新范围。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BudgetVO update(Long id, BudgetRequest request) {
        Long userId = LoginUserContext.getUserId();
        YearMonth month = parseMonth(request.getMonth());
        findOwnedBudget(id, userId);
        validateRequest(userId, request, id);
        ensureUnique(userId, month.toString(), request.getBudgetType(), request.getCategoryId(), id);
        Budget budget = toEntity(userId, month.toString(), request);
        budget.setId(id);
        budgetMapper.update(budget, new LambdaUpdateWrapper<Budget>()
                .eq(Budget::getId, id)
                .eq(Budget::getUserId, userId));
        return toVOList(userId, month, List.of(budget)).get(0);
    }

    /**
     * 删除当前用户自己的预算。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        findOwnedBudget(id, userId);
        budgetMapper.delete(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getId, id)
                .eq(Budget::getUserId, userId));
    }

    /**
     * 汇总当前用户某月预算整体进度。
     */
    @Override
    public BudgetSummaryVO summary(String month) {
        Long userId = LoginUserContext.getUserId();
        YearMonth yearMonth = parseMonth(month);
        List<BudgetVO> items = list(month);
        // 有总预算时以总预算为月度上限；没有总预算时才汇总分类预算，避免双重计算。
        BigDecimal totalBudget = items.stream()
                .filter(item -> TYPE_TOTAL.equals(item.getBudgetType()))
                .findFirst()
                .map(BudgetVO::getAmount)
                .orElseGet(() -> items.stream().map(BudgetVO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal totalUsed = totalUsedAmount(userId, yearMonth);
        BigDecimal totalRemaining = totalBudget.subtract(totalUsed);
        BigDecimal usageRate = calculateRate(totalUsed, totalBudget);
        return BudgetSummaryVO.builder()
                .month(yearMonth.toString())
                .totalBudget(totalBudget)
                .totalUsed(totalUsed)
                .totalRemaining(totalRemaining)
                .usageRate(usageRate)
                .usageStatus(resolveStatus(usageRate))
                .usageStatusLabel(resolveStatusLabel(usageRate))
                .items(items)
                .build();
    }

    /**
     * 校验预算类型、状态和分类归属。
     */
    private void validateRequest(Long userId, BudgetRequest request, Long excludeId) {
        ensureBudgetType(request.getBudgetType());
        ensureStatus(request.getStatus());
        if (TYPE_TOTAL.equals(request.getBudgetType()) && request.getCategoryId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "总预算不能选择分类");
        }
        if (TYPE_CATEGORY.equals(request.getBudgetType())) {
            if (request.getCategoryId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "分类预算必须选择分类");
            }
            Category category = categoryService.findOwnedCategory(request.getCategoryId(), userId);
            if (!"EXPENSE".equals(category.getType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "预算分类必须是支出分类");
            }
        }
    }

    /**
     * 同一用户每月只能有一个总预算，同一分类只能有一个分类预算。
     */
    private void ensureUnique(Long userId, String month, String budgetType, Long categoryId, Long excludeId) {
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, month)
                .eq(Budget::getBudgetType, budgetType);
        if (TYPE_CATEGORY.equals(budgetType)) {
            wrapper.eq(Budget::getCategoryId, categoryId);
        }
        if (excludeId != null) {
            wrapper.ne(Budget::getId, excludeId);
        }
        if (budgetMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, TYPE_TOTAL.equals(budgetType) ? "本月总预算已存在" : "本月该分类预算已存在");
        }
    }

    /**
     * 查询当前用户拥有的预算。
     */
    private Budget findOwnedBudget(Long id, Long userId) {
        Budget budget = budgetMapper.selectOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getId, id)
                .eq(Budget::getUserId, userId));
        if (budget == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预算不存在");
        }
        return budget;
    }

    /**
     * 转换预算实体。
     */
    private Budget toEntity(Long userId, String month, BudgetRequest request) {
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setMonth(month);
        budget.setCategoryId(TYPE_TOTAL.equals(request.getBudgetType()) ? null : request.getCategoryId());
        budget.setBudgetType(request.getBudgetType());
        budget.setAmount(request.getAmount());
        budget.setStatus(request.getStatus());
        budget.setDeleted(0);
        return budget;
    }

    /**
     * 批量转换预算展示对象，分类名称只从当前用户分类加载。
     */
    private List<BudgetVO> toVOList(Long userId, YearMonth month, List<Budget> budgets) {
        if (budgets.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Category> categoryMap = loadCategoryMap(userId, budgets.stream()
                .map(Budget::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return budgets.stream()
                .map(budget -> toVO(userId, month, budget, categoryMap.get(budget.getCategoryId())))
                .toList();
    }

    /**
     * 单条预算转换，使用率状态按 80% 和 100% 两条线计算。
     */
    private BudgetVO toVO(Long userId, YearMonth month, Budget budget, Category category) {
        BigDecimal used = usedAmount(userId, month, budget);
        BigDecimal remaining = budget.getAmount().subtract(used);
        BigDecimal usageRate = calculateRate(used, budget.getAmount());
        return BudgetVO.builder()
                .id(budget.getId())
                .month(budget.getMonth())
                .categoryId(budget.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .budgetType(budget.getBudgetType())
                .amount(budget.getAmount())
                .usedAmount(used)
                .remainingAmount(remaining)
                .usageRate(usageRate)
                .usageStatus(resolveStatus(usageRate))
                .usageStatusLabel(resolveStatusLabel(usageRate))
                .status(budget.getStatus())
                .build();
    }

    /**
     * 预算使用额来自支出流水，退款按同月金额抵扣支出，转账不进入查询。
     */
    private BigDecimal usedAmount(Long userId, YearMonth month, Budget budget) {
        LambdaQueryWrapper<TransactionRecord> wrapper = monthTransactionWrapper(userId, month)
                .in(TransactionRecord::getType, List.of(TRANSACTION_EXPENSE, TRANSACTION_REFUND));
        if (TYPE_CATEGORY.equals(budget.getBudgetType())) {
            wrapper.eq(TransactionRecord::getCategoryId, budget.getCategoryId());
        }
        BigDecimal used = transactionRecordMapper.selectList(wrapper).stream()
                .map(record -> TRANSACTION_REFUND.equals(record.getType()) ? record.getAmount().negate() : record.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return used.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 汇总月度总支出，退款抵扣支出。
     */
    private BigDecimal totalUsedAmount(Long userId, YearMonth month) {
        BigDecimal used = transactionRecordMapper.selectList(monthTransactionWrapper(userId, month)
                        .in(TransactionRecord::getType, List.of(TRANSACTION_EXPENSE, TRANSACTION_REFUND)))
                .stream()
                .map(record -> TRANSACTION_REFUND.equals(record.getType()) ? record.getAmount().negate() : record.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return used.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 构造月度流水范围查询。
     */
    private LambdaQueryWrapper<TransactionRecord> monthTransactionWrapper(Long userId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(LocalTime.MAX);
        return new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .ge(TransactionRecord::getTransactionTime, start)
                .le(TransactionRecord::getTransactionTime, end);
    }

    /**
     * 当前用户分类映射，避免跨用户分类名称泄露。
     */
    private Map<Long, Category> loadCategoryMap(Long userId, Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .in(Category::getId, categoryIds))
                .stream()
                .collect(Collectors.toMap(Category::getId, category -> category));
    }

    /**
     * 计算使用率百分比。
     */
    private BigDecimal calculateRate(BigDecimal used, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return used.multiply(BigDecimal.valueOf(100)).divide(amount, 4, RoundingMode.HALF_UP);
    }

    /**
     * 状态编码：NORMAL < 80%，WARNING 80%-100%，OVER > 100%。
     */
    private String resolveStatus(BigDecimal usageRate) {
        if (usageRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            return "OVER";
        }
        if (usageRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "WARNING";
        }
        return "NORMAL";
    }

    /**
     * 前端展示文案。
     */
    private String resolveStatusLabel(BigDecimal usageRate) {
        return switch (resolveStatus(usageRate)) {
            case "OVER" -> "已超支";
            case "WARNING" -> "预警";
            default -> "正常";
        };
    }

    /**
     * 预算月份只接受 yyyy-MM。
     */
    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预算月份格式必须是 yyyy-MM");
        }
    }

    /**
     * 预算类型白名单。
     */
    private void ensureBudgetType(String type) {
        if (!TYPE_TOTAL.equals(type) && !TYPE_CATEGORY.equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预算类型只支持 TOTAL 或 CATEGORY");
        }
    }

    /**
     * 预算状态只允许启用和停用。
     */
    private void ensureStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预算状态只支持 0 或 1");
        }
    }
}
