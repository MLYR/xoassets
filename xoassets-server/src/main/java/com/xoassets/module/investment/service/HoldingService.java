package com.xoassets.module.investment.service;

import com.xoassets.module.investment.dto.HoldingRequest;
import com.xoassets.module.investment.vo.HoldingDetailVO;
import com.xoassets.module.investment.vo.HoldingSummaryVO;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.module.investment.vo.InvestmentCalendarDayProfitVO;
import com.xoassets.module.investment.vo.InvestmentOverviewVO;
import com.xoassets.module.investment.vo.InvestmentTrendPointVO;
import com.xoassets.module.investment.vo.InvestmentTrendVO;
import com.xoassets.persistence.entity.Holding;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 用户投资持仓服务。
 */
public interface HoldingService {

    /**
     * 查询当前用户持仓列表。
     */
    List<HoldingVO> list();

    /**
     * 按投资模块查询当前用户持仓列表。
     */
    List<HoldingVO> list(String module);

    /**
     * 查询当前用户持仓汇总。
     */
    HoldingSummaryVO summary();

    /**
     * 查询投资总览，今日收益按今日有效价格动态汇总。
     */
    InvestmentOverviewVO overview();

    /**
     * 查询当前用户投资资产趋势。
     */
    List<InvestmentTrendPointVO> trend(LocalDate startDate, LocalDate endDate);

    /**
     * 查询投资模块资产趋势。
     */
    InvestmentTrendVO trend(String module, String period, LocalDate startDate, LocalDate endDate);

    /**
     * 查询当前用户某个持仓的详情。
     */
    HoldingDetailVO detail(Long id);

    /**
     * 查询单个持仓指定月份的收益日历。
     */
    List<InvestmentCalendarDayProfitVO> profitCalendar(Long id, YearMonth month);

    /**
     * 新增持仓。
     */
    HoldingVO create(HoldingRequest request);

    /**
     * 修改持仓基础数量和成本。
     */
    HoldingVO update(Long id, HoldingRequest request);

    /**
     * 删除当前用户持仓。
     */
    void delete(Long id);

    /**
     * 查询当前用户持仓。
     */
    Holding findOwnedHolding(Long id, Long userId);

    /**
     * 买入时按移动平均成本法增加持仓。
     */
    HoldingTradeResult applyBuy(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal price, BigDecimal fee);

    /**
     * 基金金额买入确认后，按确认份额和实际总成本增加持仓。
     */
    HoldingTradeResult applyConfirmedBuy(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal costAmount);

    /**
     * 卖出时校验数量并扣减持仓。
     */
    HoldingTradeResult applySell(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal price, BigDecimal fee);

    /**
     * 撤销买入时减少持仓数量和成本。
     */
    void revokeBuy(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal costAmount);

    /**
     * 撤销卖出时恢复持仓数量和成本。
     */
    void revokeSell(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal costAmount);
}
