package com.xoassets.module.snapshot.scheduler;

import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.module.snapshot.service.SnapshotRebuildService;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * 每日资产快照任务：失败只记录日志，不能影响主应用运行。
 */
@Slf4j
@Component
public class AssetSnapshotScheduler {

    /**
     * 资产快照服务。
     */
    private final SnapshotService snapshotService;
    /**
     * 资产快照重建服务。
     */
    private final SnapshotRebuildService snapshotRebuildService;

    /**
     * 注入定时任务依赖。
     */
    public AssetSnapshotScheduler(SnapshotService snapshotService, SnapshotRebuildService snapshotRebuildService) {
        this.snapshotService = snapshotService;
        this.snapshotRebuildService = snapshotRebuildService;
    }

    /**
     * 每天 23:50 记录所有启用用户当天资产状态，cron 可通过配置覆盖。
     */
    @XxlJob("generateDailySnapshots")
    public void generateDailySnapshots() {
        try {
            snapshotService.generateAllUsers(LocalDate.now());
        } catch (Exception exception) {
            log.error("每日资产快照生成失败", exception);
        }
    }

    /**
     * 处理补录历史流水产生的待重建快照任务，单批限制数量避免抢占业务资源。
     */
    @XxlJob("rebuildPendingAssetSnapshots")
    public void rebuildPendingAssetSnapshots() {
        try {
            snapshotRebuildService.rebuildPendingTasks(20);
        } catch (Exception exception) {
            log.error("待重建资产快照任务处理失败", exception);
        }
    }
}
