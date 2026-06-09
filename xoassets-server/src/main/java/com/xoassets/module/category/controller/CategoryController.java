package com.xoassets.module.category.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.category.dto.CategoryRequest;
import com.xoassets.module.category.dto.CategoryStatusRequest;
import com.xoassets.module.category.service.CategoryService;
import com.xoassets.module.category.vo.CategoryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类管理接口。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /**
     * 业务服务组件。
     */
    private final CategoryService categoryService;

    /**
     * 注入接口依赖。
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 查询当前用户分类，可按类型过滤。
     */
    @GetMapping
    public Result<List<CategoryVO>> list(@RequestParam(required = false) String type) {
        return Result.success(categoryService.list(type));
    }

    /**
     * 创建分类。
     */
    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategoryRequest request) {
        return Result.success(categoryService.create(request));
    }

    /**
     * 更新分类。
     */
    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return Result.success(categoryService.update(id, request));
    }

    /**
     * 删除未被流水使用的分类。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success(null);
    }

    /**
     * 启用或停用分类，已被流水使用的分类可通过停用隐藏在新增流水表单中。
     */
    @PutMapping("/{id}/status")
    public Result<CategoryVO> updateStatus(@PathVariable Long id, @Valid @RequestBody CategoryStatusRequest request) {
        return Result.success(categoryService.updateStatus(id, request.getStatus()));
    }
}
