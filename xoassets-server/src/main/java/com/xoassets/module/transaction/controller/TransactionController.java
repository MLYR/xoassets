package com.xoassets.module.transaction.controller;

import com.xoassets.common.api.PageResult;
import com.xoassets.common.api.Result;
import com.xoassets.module.transaction.dto.TransactionQuery;
import com.xoassets.module.transaction.dto.TransactionRequest;
import com.xoassets.module.transaction.service.TransactionService;
import com.xoassets.module.transaction.vo.TransactionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收支流水接口。
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    /**
     * 流水服务。
     */
    private final TransactionService transactionService;

    /**
     * 注入接口依赖。
     */
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 分页查询流水。
     */
    @GetMapping
    public Result<PageResult<TransactionVO>> page(@ModelAttribute TransactionQuery query) {
        return Result.success(transactionService.page(query));
    }

    /**
     * 查询单条流水详情。
     */
    @GetMapping("/{id}")
    public Result<TransactionVO> detail(@PathVariable Long id) {
        return Result.success(transactionService.detail(id));
    }

    /**
     * 新建流水并同步账户余额。
     */
    @PostMapping
    public Result<TransactionVO> create(@Valid @RequestBody TransactionRequest request) {
        return Result.success(transactionService.create(request));
    }

    /**
     * 修改流水并重算新旧余额影响。
     */
    @PutMapping("/{id}")
    public Result<TransactionVO> update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return Result.success(transactionService.update(id, request));
    }

    /**
     * 删除流水并回滚账户余额影响。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return Result.success(null);
    }
}
