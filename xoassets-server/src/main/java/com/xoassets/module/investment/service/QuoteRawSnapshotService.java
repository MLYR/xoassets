package com.xoassets.module.investment.service;

import java.time.LocalDate;
import java.util.List;

/**
 * 原始行情快照服务，Redis 只负责短期留存，不参与权威收益计算。
 */
public interface QuoteRawSnapshotService {

    /**
     * 写入单条原始行情快照，并给所在月份 key 设置 35 天 TTL。
     */
    void append(QuoteRawSnapshot snapshot);

    /**
     * 读取指定资产某天的原始快照，供日级价格汇总 open/high/low/close。
     */
    List<QuoteRawSnapshot> listByDate(Long assetId, LocalDate date);
}
