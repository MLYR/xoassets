package com.xoassets.module.budget.service;

import com.xoassets.module.budget.dto.BudgetRequest;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.budget.vo.BudgetVO;
import java.util.List;

/**
 * 预算服务接口。
 */
public interface BudgetService {

    /**
     * 查询当前用户某月预算列表。
     */
    List<BudgetVO> list(String month);

    /**
     * 创建预算。
     */
    BudgetVO create(BudgetRequest request);

    /**
     * 修改预算。
     */
    BudgetVO update(Long id, BudgetRequest request);

    /**
     * 删除预算。
     */
    void delete(Long id);

    /**
     * 查询当前用户某月预算汇总。
     */
    BudgetSummaryVO summary(String month);
}
