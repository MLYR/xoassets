package com.xoassets.module.investment.provider;

import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.persistence.entity.Asset;
import org.springframework.stereotype.Component;

/**
 * 手动行情 provider：手动资产不能自动刷新，只能通过手动录价写入价格快照。
 */
@Component
public class ManualQuoteProvider implements QuoteProvider {

    /**
     * 手动来源常量。
     */
    private static final String SOURCE_MANUAL = "MANUAL";

    /**
     * 判断是否支持该资产。
     */
    @Override
    public boolean supports(Asset asset) {
        return SOURCE_MANUAL.equals(asset.getQuoteSource());
    }

    /**
     * 拉取行情数据。
     */
    @Override
    public QuoteFetchResult fetch(Asset asset) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手动行情资产请使用手动更新价格");
    }
}
