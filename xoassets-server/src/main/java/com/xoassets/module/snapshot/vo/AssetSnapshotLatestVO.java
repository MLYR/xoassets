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

    /**
     * 最新快照。
     */
    private AssetSnapshotVO latest;
    /**
     * 较昨日净资产变化。
     */
    private BigDecimal netAssetChangeFromYesterday;
    /**
     * 较月初净资产变化。
     */
    private BigDecimal netAssetChangeFromMonthStart;
}
