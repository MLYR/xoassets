package com.xoassets.module.goal.service;

import com.xoassets.module.goal.dto.GoalRequest;
import com.xoassets.module.goal.vo.GoalSummaryVO;
import com.xoassets.module.goal.vo.GoalVO;
import java.util.List;

/**
 * 资产目标服务接口。
 */
public interface GoalService {

    /**
     * 查询当前用户目标列表。
     */
    List<GoalVO> list();

    /**
     * 新增目标。
     */
    GoalVO create(GoalRequest request);

    /**
     * 修改目标。
     */
    GoalVO update(Long id, GoalRequest request);

    /**
     * 删除目标。
     */
    void delete(Long id);

    /**
     * 查询目标汇总。
     */
    GoalSummaryVO summary();
}
