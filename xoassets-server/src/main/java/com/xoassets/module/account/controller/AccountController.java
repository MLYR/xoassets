package com.xoassets.module.account.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.account.dto.AccountRequest;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.account.vo.AccountVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户管理接口。
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 查询当前用户账户列表。
     */
    @GetMapping
    public Result<List<AccountVO>> list() {
        return Result.success(accountService.list());
    }

    /**
     * 创建账户。
     */
    @PostMapping
    public Result<AccountVO> create(@Valid @RequestBody AccountRequest request) {
        return Result.success(accountService.create(request));
    }

    /**
     * 更新账户基础信息。
     */
    @PutMapping("/{id}")
    public Result<AccountVO> update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return Result.success(accountService.update(id, request));
    }

    /**
     * 删除未被流水使用的账户。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Result.success(null);
    }
}
