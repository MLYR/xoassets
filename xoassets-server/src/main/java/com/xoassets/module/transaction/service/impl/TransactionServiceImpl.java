package com.xoassets.module.transaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.api.PageResult;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.category.service.CategoryService;
import com.xoassets.module.transaction.dto.TransactionQuery;
import com.xoassets.module.transaction.dto.TransactionRequest;
import com.xoassets.module.transaction.service.TransactionService;
import com.xoassets.module.transaction.vo.TransactionVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 流水服务：封装流水校验、余额变更和分页查询。
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    /**
     * 收入类型常量。
     */
    private static final String TYPE_INCOME = "INCOME";
    /**
     * 支出类型常量。
     */
    private static final String TYPE_EXPENSE = "EXPENSE";
    /**
     * 转账类型常量。
     */
    private static final String TYPE_TRANSFER = "TRANSFER";
    /**
     * 退款类型常量。
     */
    private static final String TYPE_REFUND = "REFUND";
    /**
     * 记账图片最大字节数。
     */
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    /**
     * Base64图片前缀。
     */
    private static final String DATA_IMAGE_PREFIX = "data:image/";

    /**
     * 流水数据访问组件。
     */
    private final TransactionRecordMapper transactionRecordMapper;
    /**
     * 账户数据访问组件。
     */
    private final AccountMapper accountMapper;
    /**
     * 分类数据访问组件。
     */
    private final CategoryMapper categoryMapper;
    /**
     * 账户服务。
     */
    private final AccountService accountService;
    /**
     * 业务服务组件。
     */
    private final CategoryService categoryService;

    /**
     * 注入业务依赖。
     */
    public TransactionServiceImpl(
            TransactionRecordMapper transactionRecordMapper,
            AccountMapper accountMapper,
            CategoryMapper categoryMapper,
            AccountService accountService,
            CategoryService categoryService) {
        this.transactionRecordMapper = transactionRecordMapper;
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    /**
     * 按查询条件分页返回当前用户流水，并补齐账户和分类展示名。
     */
    @Override
    public PageResult<TransactionVO> page(TransactionQuery query) {
        Long userId = LoginUserContext.getUserId();
        LambdaQueryWrapper<TransactionRecord> wrapper = buildQueryWrapper(userId, query);
        Page<TransactionRecord> page = transactionRecordMapper.selectPage(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        return toVOList(page);
    }

    /**
     * 查询当前用户自己的流水详情。
     */
    @Override
    public TransactionVO detail(Long id) {
        Long userId = LoginUserContext.getUserId();
        TransactionRecord record = findOwnedTransaction(id, userId);
        return toVO(record);
    }

    /**
     * 新增流水并立即应用余额影响，流水和余额变更必须同事务提交。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TransactionVO create(TransactionRequest request) {
        Long userId = LoginUserContext.getUserId();
        validateRequest(userId, request);
        TransactionRecord record = toEntity(userId, request);
        transactionRecordMapper.insert(record);
        applyBalance(record);
        return toVO(record);
    }

    /**
     * 修改流水时先回滚旧流水余额影响，再保存并应用新流水影响。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TransactionVO update(Long id, TransactionRequest request) {
        Long userId = LoginUserContext.getUserId();
        TransactionRecord oldRecord = findOwnedTransaction(id, userId);
        validateRequest(userId, request);
        reverseBalance(oldRecord);

        TransactionRecord newRecord = toEntity(userId, request);
        newRecord.setId(id);
        transactionRecordMapper.update(newRecord, new LambdaUpdateWrapper<TransactionRecord>()
                .eq(TransactionRecord::getId, id)
                .eq(TransactionRecord::getUserId, userId));
        applyBalance(newRecord);
        return toVO(newRecord);
    }

    /**
     * 删除流水前回滚其余额影响，保证账户余额和流水记录一致。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        TransactionRecord record = findOwnedTransaction(id, userId);
        reverseBalance(record);
        transactionRecordMapper.delete(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getId, id)
                .eq(TransactionRecord::getUserId, userId));
    }

    /**
     * 按类型、账户、分类、日期和关键词动态组装分页查询条件。
     */
    private LambdaQueryWrapper<TransactionRecord> buildQueryWrapper(Long userId, TransactionQuery query) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .orderByDesc(TransactionRecord::getTransactionTime)
                .orderByDesc(TransactionRecord::getCreatedAt);
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(TransactionRecord::getType, query.getType());
        }
        if (query.getAccountId() != null) {
            wrapper.and(w -> w.eq(TransactionRecord::getAccountId, query.getAccountId())
                    .or()
                    .eq(TransactionRecord::getTargetAccountId, query.getAccountId()));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(TransactionRecord::getCategoryId, query.getCategoryId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(TransactionRecord::getTransactionTime, LocalDateTime.of(query.getStartDate(), LocalTime.MIN));
        }
        if (query.getEndDate() != null) {
            wrapper.le(TransactionRecord::getTransactionTime, LocalDateTime.of(query.getEndDate(), LocalTime.MAX));
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(TransactionRecord::getNote, query.getKeyword());
        }
        return wrapper;
    }

    /**
     * 校验流水入参：账户归属、转账账户、分类类型和原始流水都必须合法。
     */
    private void validateRequest(Long userId, TransactionRequest request) {
        ensureType(request.getType());
        validateImageUrl(request.getImageUrl());
        accountService.findOwnedAccount(request.getAccountId(), userId);

        if (TYPE_TRANSFER.equals(request.getType())) {
            if (request.getTargetAccountId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "转账目标账户不能为空");
            }
            if (Objects.equals(request.getAccountId(), request.getTargetAccountId())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不允许同账户转账");
            }
            accountService.findOwnedAccount(request.getTargetAccountId(), userId);
            return;
        }

        if ((TYPE_INCOME.equals(request.getType()) || TYPE_EXPENSE.equals(request.getType())) && request.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收入或支出必须选择分类");
        }
        if (request.getCategoryId() != null) {
            Category category = categoryService.findOwnedCategory(request.getCategoryId(), userId);
            if (TYPE_INCOME.equals(request.getType()) && !"INCOME".equals(category.getType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "收入流水必须选择收入分类");
            }
            if (TYPE_EXPENSE.equals(request.getType()) && !"EXPENSE".equals(category.getType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "支出流水必须选择支出分类");
            }
        }
        if (request.getOriginalTransactionId() != null) {
            findOwnedTransaction(request.getOriginalTransactionId(), userId);
        }
    }

    /**
     * 前端图片以 Data URL 传入时，后端按解码后原图大小兜底校验，避免绕过前端塞入超大凭证。
     */
    private void validateImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith("data:")) {
            return;
        }
        if (!imageUrl.startsWith(DATA_IMAGE_PREFIX)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水图片只支持图片文件");
        }
        int commaIndex = imageUrl.indexOf(',');
        if (commaIndex <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水图片格式不正确");
        }
        String metadata = imageUrl.substring(0, commaIndex).toLowerCase(Locale.ROOT);
        if (!metadata.contains(";base64")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水图片格式不正确");
        }
        String payload = imageUrl.substring(commaIndex + 1);
        int payloadLength = payload.length();
        int padding = payload.endsWith("==") ? 2 : payload.endsWith("=") ? 1 : 0;
        long decodedBytes = payloadLength * 3L / 4 - padding;
        if (decodedBytes > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水图片不能超过 10MB");
        }
    }

    /**
     * 应用流水对账户余额的影响：收入/退款加余额，支出扣余额，转账双边调整。
     */
    private void applyBalance(TransactionRecord record) {
        if (TYPE_INCOME.equals(record.getType()) || TYPE_REFUND.equals(record.getType())) {
            accountService.adjustBalance(record.getUserId(), record.getAccountId(), record.getAmount());
        } else if (TYPE_EXPENSE.equals(record.getType())) {
            accountService.adjustBalance(record.getUserId(), record.getAccountId(), record.getAmount().negate());
        } else if (TYPE_TRANSFER.equals(record.getType())) {
            accountService.adjustBalance(record.getUserId(), record.getAccountId(), record.getAmount().negate());
            accountService.adjustBalance(record.getUserId(), record.getTargetAccountId(), record.getAmount());
        }
    }

    /**
     * 反向恢复流水对账户余额的影响，供修改和删除流水使用。
     */
    private void reverseBalance(TransactionRecord record) {
        if (TYPE_INCOME.equals(record.getType()) || TYPE_REFUND.equals(record.getType())) {
            accountService.adjustBalance(record.getUserId(), record.getAccountId(), record.getAmount().negate());
        } else if (TYPE_EXPENSE.equals(record.getType())) {
            accountService.adjustBalance(record.getUserId(), record.getAccountId(), record.getAmount());
        } else if (TYPE_TRANSFER.equals(record.getType())) {
            accountService.adjustBalance(record.getUserId(), record.getAccountId(), record.getAmount());
            accountService.adjustBalance(record.getUserId(), record.getTargetAccountId(), record.getAmount().negate());
        }
    }

    /**
     * 查询当前用户拥有的流水，未找到时返回业务不存在错误。
     */
    private TransactionRecord findOwnedTransaction(Long id, Long userId) {
        TransactionRecord record = transactionRecordMapper.selectOne(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getId, id)
                .eq(TransactionRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "流水不存在");
        }
        return record;
    }

    /**
     * 把请求对象转换为流水实体，amount 保持正数，方向交给 type 表达。
     */
    private TransactionRecord toEntity(Long userId, TransactionRequest request) {
        TransactionRecord record = new TransactionRecord();
        record.setUserId(userId);
        record.setType(request.getType());
        record.setAmount(request.getAmount());
        record.setAccountId(request.getAccountId());
        record.setTargetAccountId(request.getTargetAccountId());
        record.setCategoryId(request.getCategoryId());
        record.setOriginalTransactionId(request.getOriginalTransactionId());
        record.setTransactionTime(request.getTransactionTime());
        record.setNote(request.getNote());
        record.setImageUrl(request.getImageUrl());
        record.setStatus(1);
        record.setDeleted(0);
        return record;
    }

    /**
     * 批量转换分页结果，先加载账户和分类映射，避免逐条查询。
     */
    private PageResult<TransactionVO> toVOList(Page<TransactionRecord> page) {
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }
        Long userId = page.getRecords().get(0).getUserId();
        Map<Long, Account> accountMap = loadAccountMap(userId, page.getRecords().stream()
                .flatMap(record -> java.util.stream.Stream.of(record.getAccountId(), record.getTargetAccountId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, Category> categoryMap = loadCategoryMap(userId, page.getRecords().stream()
                .map(TransactionRecord::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return new PageResult<>(page.getRecords().stream()
                .map(record -> toVO(record, accountMap, categoryMap))
                .toList(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 转换单条流水展示对象。
     */
    private TransactionVO toVO(TransactionRecord record) {
        Map<Long, Account> accountMap = loadAccountMap(record.getUserId(), java.util.stream.Stream.of(record.getAccountId(), record.getTargetAccountId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, Category> categoryMap = loadCategoryMap(record.getUserId(), record.getCategoryId() == null ? Collections.emptySet() : Set.of(record.getCategoryId()));
        return toVO(record, accountMap, categoryMap);
    }

    /**
     * 使用已加载的账户和分类映射转换流水，兼顾转账目标账户名称。
     */
    private TransactionVO toVO(TransactionRecord record, Map<Long, Account> accountMap, Map<Long, Category> categoryMap) {
        Account account = accountMap.get(record.getAccountId());
        Account targetAccount = record.getTargetAccountId() == null ? null : accountMap.get(record.getTargetAccountId());
        Category category = record.getCategoryId() == null ? null : categoryMap.get(record.getCategoryId());
        return TransactionVO.builder()
                .id(record.getId())
                .type(record.getType())
                .amount(record.getAmount())
                .accountId(record.getAccountId())
                .accountName(account == null ? null : account.getName())
                .targetAccountId(record.getTargetAccountId())
                .targetAccountName(targetAccount == null ? null : targetAccount.getName())
                .categoryId(record.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .originalTransactionId(record.getOriginalTransactionId())
                .transactionTime(record.getTransactionTime())
                .note(record.getNote())
                .imageUrl(record.getImageUrl())
                .status(record.getStatus())
                .build();
    }

    /**
     * 批量加载当前用户账户映射，避免异常数据引用其他用户账户时泄露名称。
     */
    private Map<Long, Account> loadAccountMap(Long userId, Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
                        .in(Account::getId, ids))
                .stream()
                .collect(Collectors.toMap(Account::getId, account -> account));
    }

    /**
     * 批量加载当前用户分类映射，避免异常数据引用其他用户分类时泄露名称。
     */
    private Map<Long, Category> loadCategoryMap(Long userId, Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .in(Category::getId, ids))
                .stream()
                .collect(Collectors.toMap(Category::getId, category -> category));
    }

    /**
     * 第一版流水类型白名单，防止写入未定义业务类型。
     */
    private void ensureType(String type) {
        if (!TYPE_INCOME.equals(type) && !TYPE_EXPENSE.equals(type) && !TYPE_TRANSFER.equals(type) && !TYPE_REFUND.equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水类型只支持 INCOME、EXPENSE、TRANSFER、REFUND");
        }
    }
}
