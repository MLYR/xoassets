package com.xoassets.module.goal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.goal.dto.GoalRequest;
import com.xoassets.module.goal.service.GoalService;
import com.xoassets.module.goal.vo.GoalSummaryVO;
import com.xoassets.module.goal.vo.GoalVO;
import com.xoassets.persistence.entity.Goal;
import com.xoassets.persistence.mapper.GoalMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产目标服务实现：目标只归属当前登录用户。
 */
@Service
public class GoalServiceImpl implements GoalService {

    /**
     * 启用状态常量。
     */
    private static final String STATUS_ACTIVE = "ACTIVE";
    /**
     * 完成状态常量。
     */
    private static final String STATUS_DONE = "DONE";

    /**
     * 目标数据访问组件。
     */
    private final GoalMapper goalMapper;
    /**
     * 首页服务。
     */
    private final DashboardService dashboardService;

    /**
     * 注入业务依赖。
     */
    public GoalServiceImpl(GoalMapper goalMapper, DashboardService dashboardService) {
        this.goalMapper = goalMapper;
        this.dashboardService = dashboardService;
    }

    /**
     * 查询当前用户目标列表。
     */
    @Override
    public List<GoalVO> list() {
        Long userId = LoginUserContext.getUserId();
        return goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                        .eq(Goal::getUserId, userId)
                        .orderByAsc(Goal::getTargetDate)
                        .orderByDesc(Goal::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 新增目标，支持用当前净资产作为当前金额。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public GoalVO create(GoalRequest request) {
        Long userId = LoginUserContext.getUserId();
        validateRequest(request);
        Goal goal = toEntity(userId, request);
        goalMapper.insert(goal);
        return toVO(goal);
    }

    /**
     * 修改当前用户自己的目标。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public GoalVO update(Long id, GoalRequest request) {
        Long userId = LoginUserContext.getUserId();
        findOwnedGoal(id, userId);
        validateRequest(request);
        Goal goal = toEntity(userId, request);
        goal.setId(id);
        goalMapper.update(goal, new LambdaUpdateWrapper<Goal>()
                .eq(Goal::getId, id)
                .eq(Goal::getUserId, userId));
        return toVO(goal);
    }

    /**
     * 删除当前用户自己的目标。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        findOwnedGoal(id, userId);
        goalMapper.delete(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getId, id)
                .eq(Goal::getUserId, userId));
    }

    /**
     * 汇总当前用户目标金额、完成金额和目标数量。
     */
    @Override
    public GoalSummaryVO summary() {
        List<GoalVO> goals = list();
        BigDecimal totalTarget = goals.stream().map(GoalVO::getTargetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCurrent = goals.stream().map(GoalVO::getCurrentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalTarget.subtract(totalCurrent).max(BigDecimal.ZERO);
        BigDecimal completionRate = totalTarget.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalCurrent.multiply(BigDecimal.valueOf(100)).divide(totalTarget, 4, RoundingMode.HALF_UP);
        return GoalSummaryVO.builder()
                .totalTargetAmount(totalTarget)
                .totalCurrentAmount(totalCurrent)
                .totalRemainingAmount(remaining)
                .overallCompletionRate(completionRate)
                .activeGoalCount(goals.stream().filter(goal -> STATUS_ACTIVE.equals(goal.getStatus())).count())
                .completedGoalCount(goals.stream().filter(goal -> STATUS_DONE.equals(goal.getStatus())).count())
                .build();
    }

    /**
     * 校验目标状态和金额。
     */
    private void validateRequest(GoalRequest request) {
        if (!STATUS_ACTIVE.equals(request.getStatus()) && !STATUS_DONE.equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标状态只支持 ACTIVE 或 DONE");
        }
        if (Boolean.FALSE.equals(request.getUseCurrentNetAssets()) && request.getCurrentAmount() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前金额不能为空");
        }
    }

    /**
     * 查询当前用户拥有的目标。
     */
    private Goal findOwnedGoal(Long id, Long userId) {
        Goal goal = goalMapper.selectOne(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getId, id)
                .eq(Goal::getUserId, userId));
        if (goal == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标不存在");
        }
        return goal;
    }

    /**
     * 转换目标实体，useCurrentNetAssets 为 true 时使用首页净资产口径。
     */
    private Goal toEntity(Long userId, GoalRequest request) {
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(Boolean.TRUE.equals(request.getUseCurrentNetAssets())
                ? dashboardService.overview(YearMonth.now()).getNetAssets()
                : request.getCurrentAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setStatus(request.getStatus());
        goal.setDeleted(0);
        return goal;
    }

    /**
     * 转换展示对象，并计算完成率、剩余金额、剩余天数和每月需增长金额。
     */
    private GoalVO toVO(Goal goal) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        BigDecimal completionRate = goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : goal.getCurrentAmount().multiply(BigDecimal.valueOf(100)).divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP);
        long daysLeft = goal.getTargetDate() == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate()));
        BigDecimal monthsLeft = BigDecimal.valueOf(Math.max(1, (long) Math.ceil(daysLeft / 30.0)));
        return GoalVO.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus())
                .statusLabel(resolveStatusLabel(goal, completionRate))
                .completionRate(completionRate)
                .remainingAmount(remaining)
                .daysLeft(daysLeft)
                .monthlyRequiredAmount(remaining.divide(monthsLeft, 4, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * 完成率达到 100% 时展示为已完成。
     */
    private String resolveStatusLabel(Goal goal, BigDecimal completionRate) {
        if (STATUS_DONE.equals(goal.getStatus()) || completionRate.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "已完成";
        }
        return "进行中";
    }
}
