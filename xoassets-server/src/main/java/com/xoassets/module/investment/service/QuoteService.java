package com.xoassets.module.investment.service;

import com.xoassets.module.investment.dto.ManualQuoteRequest;
import com.xoassets.module.investment.vo.AssetPriceVO;
import com.xoassets.persistence.entity.AssetPrice;
import java.util.Collection;
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
     * 批量查询每个资产的最近价格。
     */
    Map<Long, AssetPrice> latestPriceMap(Collection<Long> assetIds);
}
