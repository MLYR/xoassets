package com.xoassets.module.snapshot.service;

import java.time.LocalDate;

/**
 * 资产快照重建服务：处理补录、修改、删除历史资金事件后的快照修复。
 */
public interface SnapshotRebuildService {

    /**
     * 请求重建指定用户从开始日期到今天的资产快照。
     */
    void requestRebuild(Long userId, LocalDate startDate, String triggerType);

    /**
     * 请求重建指定用户从开始日期到今天的投资日快照和资产快照。
     */
    void requestInvestmentRebuild(Long userId, LocalDate startDate, String triggerType);

    /**
     * 重建当前用户指定日期区间内的资产快照。
     */
    void rebuildCurrentUser(LocalDate startDate, LocalDate endDate);

    /**
     * 重建指定用户指定日期区间内的资产快照。
     */
    void rebuildRange(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 重建指定用户指定日期区间内的投资日快照和资产快照。
     */
    void rebuildInvestmentRange(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 批量处理待重建任务。
     */
    void rebuildPendingTasks(int limit);
}
