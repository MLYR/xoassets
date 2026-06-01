package com.xoassets.module.investment.service;

import com.xoassets.module.investment.dto.ManualQuoteRequest;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.persistence.entity.AssetPrice;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 行情价格服务。
 */
public interface QuoteService {

    /**
     * 手动写入资产价格。
     */
    AssetPriceVO manualQuote(ManualQuoteRequest request);

    /**
     * 根据资产行情来源刷新价格。
     */
    AssetPriceVO refreshQuote(Long assetId);

    /**
     * 批量刷新行情，单个资产失败时保留旧价格并继续处理其他资产。
     */
    List<AssetPriceVO> refreshQuotes(Collection<Long> assetIds);

    /**
     * 刷新过期价格，仍新鲜时直接返回最近快照。
     */
    AssetPriceVO refreshQuoteIfStale(Long assetId);

    /**
     * 批量查询每个资产的最近价格。
     */
    Map<Long, AssetPrice> latestPriceMap(Collection<Long> assetIds);
}
