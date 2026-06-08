package com.xoassets.module.snapshot.service;

import com.xoassets.module.snapshot.vo.AssetSnapshotLatestVO;
import com.xoassets.module.snapshot.vo.AssetSnapshotVO;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产快照服务：负责生成、查询和定时批量落库。
 */
public interface SnapshotService {

    /**
     * 查询当前用户最新资产快照，并计算相对昨日和本月初变化。
     */
    AssetSnapshotLatestVO latest();

    /**
     * 查询当前用户指定日期区间内的资产快照趋势。
     */
    List<AssetSnapshotVO> trend(LocalDate startDate, LocalDate endDate);

    /**
     * 手动生成或更新当前用户今天的资产快照。
     */
    AssetSnapshotVO generateToday();

    /**
     * 手动生成或更新当前用户指定日期的资产快照。
     */
    AssetSnapshotVO generate(LocalDate snapshotDate);

    /**
     * 生成指定用户指定日期快照，供定时任务和内部复用。
     */
    AssetSnapshotVO generateForUser(Long userId, LocalDate snapshotDate);

    /**
     * 为所有启用用户生成指定日期快照。
     */
    void generateAllUsers(LocalDate snapshotDate);
}
