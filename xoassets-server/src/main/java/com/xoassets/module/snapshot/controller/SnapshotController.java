package com.xoassets.module.snapshot.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.snapshot.service.SnapshotRebuildService;
import com.xoassets.module.snapshot.service.SnapshotService;
import com.xoassets.module.snapshot.vo.AssetSnapshotLatestVO;
import com.xoassets.module.snapshot.vo.AssetSnapshotVO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资产快照接口：为首页、趋势图和本地开发手动生成提供数据。
 */
@RestController
@RequestMapping("/api/snapshots")
public class SnapshotController {

    /**
     * 资产快照服务。
     */
    private final SnapshotService snapshotService;
    /**
     * 资产快照重建服务。
     */
    private final SnapshotRebuildService snapshotRebuildService;

    /**
     * 注入接口依赖。
     */
    public SnapshotController(SnapshotService snapshotService, SnapshotRebuildService snapshotRebuildService) {
        this.snapshotService = snapshotService;
        this.snapshotRebuildService = snapshotRebuildService;
    }

    /**
     * 查询当前用户最新快照。
     */
    @GetMapping("/latest")
    public Result<AssetSnapshotLatestVO> latest() {
        return Result.success(snapshotService.latest());
    }

    /**
     * 查询当前用户快照趋势，不传区间默认由服务层使用最近 30 天。
     */
    @GetMapping("/trend")
    public Result<List<AssetSnapshotVO>> trend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(snapshotService.trend(startDate, endDate));
    }

    /**
     * 手动生成或更新今天的快照，便于本地开发和验收。
     */
    @PostMapping("/generate-today")
    public Result<AssetSnapshotVO> generateToday() {
        return Result.success(snapshotService.generateToday());
    }

    /**
     * 手动生成或更新指定日期快照，便于本地对账修复历史数据。
     */
    @PostMapping("/generate")
    public Result<AssetSnapshotVO> generate(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate snapshotDate) {
        return Result.success(snapshotService.generate(snapshotDate));
    }

    /**
     * 手动重建指定区间资产快照，适合批量补录或导入后对账修复。
     */
    @PostMapping("/rebuild")
    public Result<Void> rebuild(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        snapshotRebuildService.rebuildCurrentUser(startDate, endDate);
        return Result.success(null);
    }
}
