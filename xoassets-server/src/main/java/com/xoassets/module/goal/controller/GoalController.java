package com.xoassets.module.goal.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.goal.dto.GoalRequest;
import com.xoassets.module.goal.service.GoalService;
import com.xoassets.module.goal.vo.GoalSummaryVO;
import com.xoassets.module.goal.vo.GoalVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资产目标接口。
 */
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    /**
     * 资产目标服务。
     */
    private final GoalService goalService;

    /**
     * 注入接口依赖。
     */
    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    /**
     * 查询当前用户目标列表。
     */
    @GetMapping
    public Result<List<GoalVO>> list() {
        return Result.success(goalService.list());
    }

    /**
     * 新增目标。
     */
    @PostMapping
    public Result<GoalVO> create(@Valid @RequestBody GoalRequest request) {
        return Result.success(goalService.create(request));
    }

    /**
     * 修改目标。
     */
    @PutMapping("/{id}")
    public Result<GoalVO> update(@PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        return Result.success(goalService.update(id, request));
    }

    /**
     * 删除目标。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return Result.success(null);
    }

    /**
     * 查询目标汇总。
     */
    @GetMapping("/summary")
    public Result<GoalSummaryVO> summary() {
        return Result.success(goalService.summary());
    }
}
