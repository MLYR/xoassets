package com.xoassets.module.snapshot.scheduler;

import com.xoassets.module.snapshot.service.SnapshotService;
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
     * 注入定时任务依赖。
     */
    public AssetSnapshotScheduler(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
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
}
