package com.xoassets.module.investment.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 投资模块趋势返回对象，支持总资产、基金、股票和虚拟货币独立切换。
 */
@Data
@Builder
public class InvestmentTrendVO {

    /**
     * 投资模块。
     */
    private String module;
    /**
     * 统计周期。
     */
    private String period;
    /**
     * 趋势点列表。
     */
    private List<InvestmentTrendPointVO> points;
}
