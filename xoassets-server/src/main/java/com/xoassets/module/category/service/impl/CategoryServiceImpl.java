package com.xoassets.module.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.category.dto.CategoryRequest;
import com.xoassets.module.category.service.CategoryService;
import com.xoassets.module.category.vo.CategoryVO;
import com.xoassets.persistence.entity.Category;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.CategoryMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 分类服务：第一期分类完全归属用户，新用户注册时复制一组默认分类。
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    /**
     * 收入类型常量。
     */
    private static final String TYPE_INCOME = "INCOME";
    /**
     * 支出类型常量。
     */
    private static final String TYPE_EXPENSE = "EXPENSE";
    /**
     * 默认支出分类。
     */
    private static final List<String> DEFAULT_EXPENSE_CATEGORIES = List.of("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "生活缴费", "其他");
    /**
     * 默认收入分类。
     */
    private static final List<String> DEFAULT_INCOME_CATEGORIES = List.of("工资", "奖金", "副业", "理财收益", "红包", "退款", "其他");

    /**
     * 分类数据访问组件。
     */
    private final CategoryMapper categoryMapper;
    /**
     * 流水数据访问组件。
     */
    private final TransactionRecordMapper transactionRecordMapper;

    /**
     * 注入业务依赖。
     */
    public CategoryServiceImpl(CategoryMapper categoryMapper, TransactionRecordMapper transactionRecordMapper) {
        this.categoryMapper = categoryMapper;
        this.transactionRecordMapper = transactionRecordMapper;
    }

    /**
     * 查询当前用户分类，可按收入或支出类型过滤。
     */
    @Override
    public List<CategoryVO> list(String type) {
        Long userId = LoginUserContext.getUserId();
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .orderByAsc(Category::getSortOrder)
                .orderByDesc(Category::getCreatedAt);
        if (StringUtils.hasText(type)) {
            wrapper.eq(Category::getType, type);
        }
        return categoryMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /**
     * 新建分类前校验分类类型，避免写入前端无法识别的数据。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CategoryVO create(CategoryRequest request) {
        Long userId = LoginUserContext.getUserId();
        ensureType(request.getType());
        ensureStatus(request.getStatus());
        ensureNameUnique(userId, request.getType(), request.getName(), null);
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setStatus(request.getStatus());
        category.setSortOrder(request.getSortOrder());
        category.setDeleted(0);
        categoryMapper.insert(category);
        return toVO(category);
    }

    /**
     * 更新当前用户自己的分类，禁止跨用户修改。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CategoryVO update(Long id, CategoryRequest request) {
        Long userId = LoginUserContext.getUserId();
        ensureType(request.getType());
        ensureStatus(request.getStatus());
        Category category = findOwnedCategory(id, userId);
        ensureNameUnique(userId, request.getType(), request.getName(), id);
        category.setName(request.getName());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setStatus(request.getStatus());
        category.setSortOrder(request.getSortOrder());
        categoryMapper.update(category, new LambdaUpdateWrapper<Category>()
                .eq(Category::getId, id)
                .eq(Category::getUserId, userId));
        return toVO(category);
    }

    /**
     * 删除分类前检查是否被流水使用，避免历史数据展示缺失分类名。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        findOwnedCategory(id, userId);
        Long transactionCount = transactionRecordMapper.selectCount(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .eq(TransactionRecord::getCategoryId, id));
        if (transactionCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分类已有流水，不能删除，可停用");
        }
        categoryMapper.delete(new LambdaQueryWrapper<Category>().eq(Category::getId, id).eq(Category::getUserId, userId));
    }

    /**
     * 启用或停用当前用户自己的分类，停用后不会出现在新增流水分类下拉中。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CategoryVO updateStatus(Long id, Integer status) {
        Long userId = LoginUserContext.getUserId();
        ensureStatus(status);
        Category category = findOwnedCategory(id, userId);
        category.setStatus(status);
        categoryMapper.update(category, new LambdaUpdateWrapper<Category>()
                .eq(Category::getId, id)
                .eq(Category::getUserId, userId));
        return toVO(category);
    }

    /**
     * 查询当前用户拥有的分类，供流水校验复用。
     */
    @Override
    public Category findOwnedCategory(Long id, Long userId) {
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, id)
                .eq(Category::getUserId, userId));
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    /**
     * 为注册成功的新用户创建默认分类；注册事务回滚时这些分类一起回滚。
     */
    @Override
    public void initializeDefaultCategories(Long userId) {
        insertDefaultCategories(userId, TYPE_EXPENSE, DEFAULT_EXPENSE_CATEGORIES);
        insertDefaultCategories(userId, TYPE_INCOME, DEFAULT_INCOME_CATEGORIES);
    }

    /**
     * 批量写入默认分类，排序按默认数组顺序稳定展示。
     */
    private void insertDefaultCategories(Long userId, String type, List<String> names) {
        for (int index = 0; index < names.size(); index++) {
            Category category = new Category();
            category.setUserId(userId);
            category.setName(names.get(index));
            category.setType(type);
            category.setStatus(1);
            category.setSortOrder(index + 1);
            category.setDeleted(0);
            categoryMapper.insert(category);
        }
    }

    /**
     * 第一版分类类型只支持收入和支出。
     */
    private void ensureType(String type) {
        if (!TYPE_INCOME.equals(type) && !TYPE_EXPENSE.equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类类型只支持 INCOME 或 EXPENSE");
        }
    }

    /**
     * 分类状态只允许启用和停用两个值。
     */
    private void ensureStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类状态只支持 0 或 1");
        }
    }

    /**
     * 同一用户、同一类型、未删除分类下名称不能重复。
     */
    private void ensureNameUnique(Long userId, String type, String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .eq(Category::getType, type)
                .eq(Category::getName, name);
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "同类型分类名称已存在");
        }
    }

    /**
     * 转换为分类展示对象。
     */
    private CategoryVO toVO(Category category) {
        return CategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .color(category.getColor())
                .status(category.getStatus())
                .sortOrder(category.getSortOrder())
                .build();
    }
}
