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
 * 分类服务：第一期分类完全归属用户，不做系统默认分类复制。
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final TransactionRecordMapper transactionRecordMapper;

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
        Category category = findOwnedCategory(id, userId);
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
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分类已有流水，第一版不允许删除");
        }
        categoryMapper.delete(new LambdaQueryWrapper<Category>().eq(Category::getId, id).eq(Category::getUserId, userId));
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
     * 第一版分类类型只支持收入和支出。
     */
    private void ensureType(String type) {
        if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类类型只支持 INCOME 或 EXPENSE");
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
