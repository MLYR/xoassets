package com.xoassets.module.snapshot.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 首页最新快照对象，额外提供昨日和本月初变化值。
 */
@Data
@Builder
public class AssetSnapshotLatestVO {

    private AssetSnapshotVO latest;
    private BigDecimal netAssetChangeFromYesterday;
    private BigDecimal netAssetChangeFromMonthStart;
}
