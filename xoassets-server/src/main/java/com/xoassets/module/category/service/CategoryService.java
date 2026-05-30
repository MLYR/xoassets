package com.xoassets.module.category.service;

import com.xoassets.module.category.dto.CategoryRequest;
import com.xoassets.module.category.vo.CategoryVO;
import com.xoassets.persistence.entity.Category;
import java.util.List;

/**
 * 分类服务接口：提供分类 CRUD 和流水分类归属校验能力。
 */
public interface CategoryService {

    /**
     * 查询分类列表，可按类型过滤。
     */
    List<CategoryVO> list(String type);

    /**
     * 创建分类。
     */
    CategoryVO create(CategoryRequest request);

    /**
     * 更新分类。
     */
    CategoryVO update(Long id, CategoryRequest request);

    /**
     * 删除分类。
     */
    void delete(Long id);

    /**
     * 查询指定用户拥有的分类。
     */
    Category findOwnedCategory(Long id, Long userId);

    /**
     * 为新用户初始化默认收支分类，必须由注册事务调用。
     */
    void initializeDefaultCategories(Long userId);
}
