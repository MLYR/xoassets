package com.xoassets.module.exchange.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.exchange.service.ExchangeRateService;
import com.xoassets.module.exchange.vo.ExchangeRateVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 汇率接口，前端只读取展示换算所需的缓存汇率。
 */
@RestController
@RequestMapping("/api/exchange-rates")
public class ExchangeRateController {

    /**
     * 汇率服务。
     */
    private final ExchangeRateService exchangeRateService;

    /**
     * 注入接口依赖。
     */
    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * 查询 USD/CNY 汇率，失败时服务层返回最近缓存或默认值。
     */
    @GetMapping("/usd-cny")
    public Result<ExchangeRateVO> usdCny() {
        return Result.success(exchangeRateService.usdCny());
    }
}
