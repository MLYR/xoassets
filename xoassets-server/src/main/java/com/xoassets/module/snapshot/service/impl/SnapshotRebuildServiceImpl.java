package com.xoassets.module.snapshot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.investment.scheduler.InvestmentDailySnapshotJob;
import com.xoassets.module.snapshot.service.SnapshotRebuildService;
import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.persistence.entity.SnapshotRebuildTask;
import com.xoassets.persistence.mapper.SnapshotRebuildTaskMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 资产快照重建服务实现：近期变动同步重算，长跨度历史变动合并为后台任务。
 */
@Slf4j
@Service
public class SnapshotRebuildServiceImpl implements SnapshotRebuildService {

    /**
     * 近期变动同步重算的最大天数，覆盖日常“过几天补记”场景。
     */
    private static final long SYNC_REBUILD_MAX_DAYS = 31;
    /**
     * 默认任务批量处理上限。
     */
    private static final int DEFAULT_TASK_LIMIT = 20;
    /**
     * 待处理状态。
     */
    private static final String STATUS_PENDING = "PENDING";
    /**
     * 处理中状态。
     */
    private static final String STATUS_RUNNING = "RUNNING";
    /**
     * 成功状态。
     */
    private static final String STATUS_SUCCESS = "SUCCESS";
    /**
     * 失败状态。
     */
    private static final String STATUS_FAILED = "FAILED";
    /**
     * 运行中任务超过该分钟数未完成时允许重新捞起，避免进程中断后永久卡住。
     */
    private static final long RUNNING_STALE_MINUTES = 30;
    /**
     * 投资重建触发来源前缀。
     */
    private static final String TRIGGER_INVESTMENT_PREFIX = "INVESTMENT";

    /**
     * 资产快照服务。
     */
    private final SnapshotService snapshotService;
    /**
     * 投资日快照任务。
     */
    private final InvestmentDailySnapshotJob investmentDailySnapshotJob;
    /**
     * 重建任务数据访问组件。
     */
    private final SnapshotRebuildTaskMapper taskMapper;

    /**
     * 注入业务依赖。
     */
    public SnapshotRebuildServiceImpl(
            SnapshotService snapshotService,
            InvestmentDailySnapshotJob investmentDailySnapshotJob,
            SnapshotRebuildTaskMapper taskMapper) {
        this.snapshotService = snapshotService;
        this.investmentDailySnapshotJob = investmentDailySnapshotJob;
        this.taskMapper = taskMapper;
    }

    /**
     * 近期请求直接重建；长跨度请求合并为待处理任务，避免补录老流水阻塞用户操作。
     */
    @Override
    public void requestRebuild(Long userId, LocalDate startDate, String triggerType) {
        requestRebuild(userId, startDate, triggerType, false);
    }

    /**
     * 投资交易变更必须先重建投资日快照，再重建资产快照，保证投资趋势和净资产口径一致。
     */
    @Override
    public void requestInvestmentRebuild(Long userId, LocalDate startDate, String triggerType) {
        requestRebuild(userId, startDate, triggerType, true);
    }

    /**
     * 近期请求直接重建；长跨度请求合并为待处理任务，避免补录老流水阻塞用户操作。
     */
    private void requestRebuild(Long userId, LocalDate startDate, String triggerType, boolean rebuildInvestment) {
        LocalDate today = LocalDate.now();
        LocalDate start = normalizeStart(startDate, today);
        if (userId == null || start == null) {
            return;
        }
        if (daysBetween(start, today) <= SYNC_REBUILD_MAX_DAYS) {
            try {
                if (rebuildInvestment) {
                    rebuildInvestmentRange(userId, start, today);
                } else {
                    rebuildRange(userId, start, today);
                }
                return;
            } catch (Exception exception) {
                log.warn("近期快照同步重建失败，转为后台任务 userId={} startDate={} triggerType={}", userId, start, triggerType, exception);
            }
        }
        mergePendingTask(userId, start, today, normalizedTriggerType(triggerType, rebuildInvestment));
    }

    /**
     * 手动重建当前登录用户快照区间。
     */
    @Override
    public void rebuildCurrentUser(LocalDate startDate, LocalDate endDate) {
        rebuildRange(LoginUserContext.getUserId(), startDate, endDate);
    }

    /**
     * 按日期逐日 upsert 资产快照；快照服务内部会按快照日重建历史账户和投资资产。
     */
    @Override
    public void rebuildRange(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate start = normalizeStart(startDate, today);
        LocalDate end = normalizeEnd(endDate, today);
        if (userId == null || start == null || end == null || start.isAfter(end)) {
            return;
        }
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            snapshotService.generateForUser(userId, cursor);
            cursor = cursor.plusDays(1);
        }
    }

    /**
     * 按日期逐日先 upsert 投资日快照，再 upsert 资产快照，覆盖投资补录导致的持仓和净资产历史差异。
     */
    @Override
    public void rebuildInvestmentRange(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate start = normalizeStart(startDate, today);
        LocalDate end = normalizeEnd(endDate, today);
        if (userId == null || start == null || end == null || start.isAfter(end)) {
            return;
        }
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            investmentDailySnapshotJob.snapshotForUser(userId, cursor);
            snapshotService.generateForUser(userId, cursor);
            cursor = cursor.plusDays(1);
        }
    }

    /**
     * 处理待重建任务；单任务失败只标记失败，后续请求会合并并重试。
     */
    @Override
    public void rebuildPendingTasks(int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_TASK_LIMIT : limit;
        LocalDateTime staleTime = LocalDateTime.now().minusMinutes(RUNNING_STALE_MINUTES);
        List<SnapshotRebuildTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<SnapshotRebuildTask>()
                .and(wrapper -> wrapper.in(SnapshotRebuildTask::getStatus, STATUS_PENDING, STATUS_FAILED)
                        .or()
                        .eq(SnapshotRebuildTask::getStatus, STATUS_RUNNING)
                        .lt(SnapshotRebuildTask::getLastRunAt, staleTime))
                .orderByAsc(SnapshotRebuildTask::getUpdatedAt)
                .last("limit " + safeLimit));
        for (SnapshotRebuildTask task : tasks) {
            runTask(task);
        }
    }

    /**
     * 执行单个重建任务并记录结果。
     */
    private void runTask(SnapshotRebuildTask task) {
        LocalDateTime now = LocalDateTime.now();
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<SnapshotRebuildTask>()
                .set(SnapshotRebuildTask::getStatus, STATUS_RUNNING)
                .set(SnapshotRebuildTask::getRetryCount, retryCount + 1)
                .set(SnapshotRebuildTask::getLastRunAt, now)
                .set(SnapshotRebuildTask::getErrorMessage, null)
                .eq(SnapshotRebuildTask::getId, task.getId())
                .and(wrapper -> wrapper.in(SnapshotRebuildTask::getStatus, STATUS_PENDING, STATUS_FAILED)
                        .or()
                        .eq(SnapshotRebuildTask::getStatus, STATUS_RUNNING)
                        .lt(SnapshotRebuildTask::getLastRunAt, now.minusMinutes(RUNNING_STALE_MINUTES))));
        if (updated == 0) {
            return;
        }
        try {
            if (requiresInvestmentRebuild(task.getTriggerType())) {
                rebuildInvestmentRange(task.getUserId(), task.getStartDate(), task.getEndDate());
            } else {
                rebuildRange(task.getUserId(), task.getStartDate(), task.getEndDate());
            }
            taskMapper.update(null, new LambdaUpdateWrapper<SnapshotRebuildTask>()
                    .set(SnapshotRebuildTask::getStatus, STATUS_SUCCESS)
                    .set(SnapshotRebuildTask::getErrorMessage, null)
                    .eq(SnapshotRebuildTask::getId, task.getId()));
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            taskMapper.update(null, new LambdaUpdateWrapper<SnapshotRebuildTask>()
                    .set(SnapshotRebuildTask::getStatus, STATUS_FAILED)
                    .set(SnapshotRebuildTask::getErrorMessage, message.length() > 255 ? message.substring(0, 255) : message)
                    .eq(SnapshotRebuildTask::getId, task.getId()));
            log.warn("资产快照重建任务失败 taskId={} userId={} startDate={} endDate={}",
                    task.getId(), task.getUserId(), task.getStartDate(), task.getEndDate(), exception);
        }
    }

    /**
     * 合并同一用户待处理任务，保留最早开始日期和最晚结束日期。
     */
    private void mergePendingTask(Long userId, LocalDate startDate, LocalDate endDate, String triggerType) {
        SnapshotRebuildTask exists = taskMapper.selectOne(new LambdaQueryWrapper<SnapshotRebuildTask>()
                .eq(SnapshotRebuildTask::getUserId, userId)
                .in(SnapshotRebuildTask::getStatus, STATUS_PENDING, STATUS_FAILED)
                .orderByAsc(SnapshotRebuildTask::getStartDate)
                .last("limit 1"));
        if (exists == null) {
            SnapshotRebuildTask task = new SnapshotRebuildTask();
            task.setUserId(userId);
            task.setStartDate(startDate);
            task.setEndDate(endDate);
            task.setStatus(STATUS_PENDING);
            task.setTriggerType(triggerType == null ? "UNKNOWN" : triggerType);
            task.setRetryCount(0);
            task.setDeleted(0);
            taskMapper.insert(task);
            return;
        }
        LocalDate mergedStart = exists.getStartDate().isAfter(startDate) ? startDate : exists.getStartDate();
        LocalDate mergedEnd = exists.getEndDate().isBefore(endDate) ? endDate : exists.getEndDate();
        taskMapper.update(null, new LambdaUpdateWrapper<SnapshotRebuildTask>()
                .set(SnapshotRebuildTask::getStartDate, mergedStart)
                .set(SnapshotRebuildTask::getEndDate, mergedEnd)
                .set(SnapshotRebuildTask::getStatus, STATUS_PENDING)
                .set(SnapshotRebuildTask::getTriggerType, mergeTriggerType(exists.getTriggerType(), triggerType))
                .set(SnapshotRebuildTask::getErrorMessage, null)
                .eq(SnapshotRebuildTask::getId, exists.getId()));
    }

    /**
     * 规范触发来源；投资触发统一保留 INVESTMENT 前缀，后台可据此补跑投资日快照。
     */
    private String normalizedTriggerType(String triggerType, boolean rebuildInvestment) {
        if (rebuildInvestment && (triggerType == null || !triggerType.startsWith(TRIGGER_INVESTMENT_PREFIX))) {
            return TRIGGER_INVESTMENT_PREFIX;
        }
        return triggerType == null ? "UNKNOWN" : triggerType;
    }

    /**
     * 合并任务来源；任一来源需要投资重建时，合并任务也必须走投资重建路径。
     */
    private String mergeTriggerType(String existsTriggerType, String newTriggerType) {
        if (requiresInvestmentRebuild(existsTriggerType) || requiresInvestmentRebuild(newTriggerType)) {
            return requiresInvestmentRebuild(newTriggerType) ? newTriggerType : TRIGGER_INVESTMENT_PREFIX;
        }
        return newTriggerType == null ? existsTriggerType : newTriggerType;
    }

    /**
     * 判断任务是否需要先补投资日快照。
     */
    private boolean requiresInvestmentRebuild(String triggerType) {
        return triggerType != null && triggerType.startsWith(TRIGGER_INVESTMENT_PREFIX);
    }

    /**
     * 规范开始日期；未来日期不需要立即修复历史快照。
     */
    private LocalDate normalizeStart(LocalDate startDate, LocalDate today) {
        if (startDate == null || startDate.isAfter(today)) {
            return null;
        }
        return startDate;
    }

    /**
     * 规范结束日期，禁止生成未来快照。
     */
    private LocalDate normalizeEnd(LocalDate endDate, LocalDate today) {
        if (endDate == null || endDate.isAfter(today)) {
            return today;
        }
        return endDate;
    }

    /**
     * 计算包含首尾日期的自然日数量。
     */
    private long daysBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}
