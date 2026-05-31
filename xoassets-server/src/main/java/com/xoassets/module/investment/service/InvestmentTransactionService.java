package com.xoassets.module.investment.service;

import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.vo.InvestmentTransactionVO;
import java.util.List;

/**
 * 投资交易服务。
 */
public interface InvestmentTransactionService {

    /**
     * 创建买入或卖出交易，并联动持仓。
     */
    InvestmentTransactionVO create(InvestmentTransactionRequest request);

    /**
     * 查询当前用户投资交易。
     */
    List<InvestmentTransactionVO> list(Long holdingId);
}
