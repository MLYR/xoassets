package com.xoassets.module.account.service;

import com.xoassets.module.account.dto.AccountRequest;
import com.xoassets.module.account.vo.AccountVO;
import com.xoassets.persistence.entity.Account;
import java.math.BigDecimal;
import java.util.List;

/**
 * 账户服务接口：暴露账户 CRUD 和流水余额修正所需的账户能力。
 */
public interface AccountService {

    /**
     * 查询当前用户账户列表。
     */
    List<AccountVO> list();

    /**
     * 创建账户。
     */
    AccountVO create(AccountRequest request);

    /**
     * 更新账户基础信息。
     */
    AccountVO update(Long id, AccountRequest request);

    /**
     * 删除账户。
     */
    void delete(Long id);

    /**
     * 查询指定用户拥有的账户。
     */
    Account findOwnedAccount(Long id, Long userId);

    /**
     * 调整账户余额，delta 可正可负。
     */
    void adjustBalance(Long userId, Long accountId, BigDecimal delta);
}
