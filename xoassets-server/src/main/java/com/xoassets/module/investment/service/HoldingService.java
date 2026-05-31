package com.xoassets.module.investment.service;

import com.xoassets.module.investment.dto.HoldingRequest;
import com.xoassets.module.investment.vo.HoldingSummaryVO;
import com.xoassets.module.investment.vo.HoldingVO;
import com.xoassets.persistence.entity.Holding;
import java.math.BigDecimal;
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
     * 查询当前用户持仓汇总。
     */
    HoldingSummaryVO summary();

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
     * 卖出时校验数量并扣减持仓。
     */
    HoldingTradeResult applySell(Long userId, Long holdingId, Long assetId, BigDecimal quantity, BigDecimal price, BigDecimal fee);
}
