package com.xoassets.module.investment.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.investment.dto.ManualQuoteRequest;
import com.xoassets.module.investment.dto.RefreshQuoteRequest;
import com.xoassets.module.investment.service.QuoteService;
import com.xoassets.module.investment.vo.AssetPriceVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行情价格接口。
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * 手动更新价格。
     */
    @PostMapping("/manual")
    public Result<AssetPriceVO> manualQuote(@Valid @RequestBody ManualQuoteRequest request) {
        return Result.success(quoteService.manualQuote(request));
    }

    /**
     * 根据资产配置刷新行情。
     */
    @PostMapping("/refresh")
    public Result<AssetPriceVO> refreshQuote(@Valid @RequestBody RefreshQuoteRequest request) {
        return Result.success(quoteService.refreshQuote(request.getAssetId()));
    }
}
