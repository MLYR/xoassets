package com.xoassets.module.investment.service;

import com.xoassets.module.investment.dto.InvestmentTransactionRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionConvertRequest;
import com.xoassets.module.investment.dto.InvestmentTransactionRevokeRequest;
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
     * 转换持仓：同一事务内卖出源持仓并买入目标持仓。
     */
    List<InvestmentTransactionVO> convert(InvestmentTransactionConvertRequest request);

    /**
     * 查询当前用户投资交易。
     */
    List<InvestmentTransactionVO> list(Long holdingId);

    /**
     * 撤销当前用户自己的投资交易，并反向恢复账户余额和持仓。
     */
    InvestmentTransactionVO revoke(Long id, InvestmentTransactionRevokeRequest request);

    /**
     * 确认待确认基金买入交易，供定时任务调用。
     */
    void confirmPendingFundBuys();
}
