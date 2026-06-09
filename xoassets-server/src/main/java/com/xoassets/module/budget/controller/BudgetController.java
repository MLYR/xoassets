package com.xoassets.module.budget.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.budget.dto.BudgetRequest;
import com.xoassets.module.budget.service.BudgetService;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.budget.vo.BudgetVO;
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
 * 预算管理接口。
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    /**
     * 预算服务。
     */
    private final BudgetService budgetService;

    /**
     * 注入接口依赖。
     */
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * 查询当前用户月度预算。
     */
    @GetMapping
    public Result<List<BudgetVO>> list(@RequestParam String month) {
        return Result.success(budgetService.list(month));
    }

    /**
     * 新增预算。
     */
    @PostMapping
    public Result<BudgetVO> create(@Valid @RequestBody BudgetRequest request) {
        return Result.success(budgetService.create(request));
    }

    /**
     * 修改预算。
     */
    @PutMapping("/{id}")
    public Result<BudgetVO> update(@PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        return Result.success(budgetService.update(id, request));
    }

    /**
     * 删除预算。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return Result.success(null);
    }

    /**
     * 查询当前用户月度预算汇总。
     */
    @GetMapping("/summary")
    public Result<BudgetSummaryVO> summary(@RequestParam String month) {
        return Result.success(budgetService.summary(month));
    }
}
