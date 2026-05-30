package com.xoassets.module.transaction.service;

import com.xoassets.common.api.PageResult;
import com.xoassets.module.transaction.dto.TransactionQuery;
import com.xoassets.module.transaction.dto.TransactionRequest;
import com.xoassets.module.transaction.vo.TransactionVO;

/**
 * 流水服务接口：封装分页、详情和余额一致性相关写操作。
 */
public interface TransactionService {

    /**
     * 分页查询流水。
     */
    PageResult<TransactionVO> page(TransactionQuery query);

    /**
     * 查询流水详情。
     */
    TransactionVO detail(Long id);

    /**
     * 创建流水。
     */
    TransactionVO create(TransactionRequest request);

    /**
     * 更新流水。
     */
    TransactionVO update(Long id, TransactionRequest request);

    /**
     * 删除流水。
     */
    void delete(Long id);
}
