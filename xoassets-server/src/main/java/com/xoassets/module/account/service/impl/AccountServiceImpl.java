package com.xoassets.module.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.account.dto.AccountRequest;
import com.xoassets.module.account.service.AccountService;
import com.xoassets.module.account.vo.AccountVO;
import com.xoassets.persistence.entity.Account;
import com.xoassets.persistence.entity.TransactionRecord;
import com.xoassets.persistence.mapper.AccountMapper;
import com.xoassets.persistence.mapper.TransactionRecordMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户服务：所有读写都按当前 user_id 过滤，防止越权。
 */
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final TransactionRecordMapper transactionRecordMapper;

    public AccountServiceImpl(AccountMapper accountMapper, TransactionRecordMapper transactionRecordMapper) {
        this.accountMapper = accountMapper;
        this.transactionRecordMapper = transactionRecordMapper;
    }

    /**
     * 查询当前用户的账户列表，按用户自定义排序优先展示。
     */
    @Override
    public List<AccountVO> list() {
        Long userId = LoginUserContext.getUserId();
        return accountMapper.selectList(new LambdaQueryWrapper<Account>()
                        .eq(Account::getUserId, userId)
                        .orderByAsc(Account::getSortOrder)
                        .orderByDesc(Account::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 新建账户时初始余额同步作为当前余额，后续余额由流水统一调整。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AccountVO create(AccountRequest request) {
        Long userId = LoginUserContext.getUserId();
        Account account = new Account();
        account.setUserId(userId);
        account.setName(request.getName());
        account.setType(request.getType());
        account.setInitialBalance(request.getInitialBalance());
        account.setBalance(request.getInitialBalance());
        account.setCurrency(request.getCurrency());
        account.setStatus(request.getStatus());
        account.setSortOrder(request.getSortOrder());
        account.setRemark(request.getRemark());
        account.setDeleted(0);
        accountMapper.insert(account);
        return toVO(account);
    }

    /**
     * 更新账户基础信息，不直接改当前余额，避免绕过流水口径。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AccountVO update(Long id, AccountRequest request) {
        Long userId = LoginUserContext.getUserId();
        Account account = findOwnedAccount(id, userId);
        account.setName(request.getName());
        account.setType(request.getType());
        account.setCurrency(request.getCurrency());
        account.setStatus(request.getStatus());
        account.setSortOrder(request.getSortOrder());
        account.setRemark(request.getRemark());
        accountMapper.update(account, new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, id)
                .eq(Account::getUserId, userId));
        return toVO(account);
    }

    /**
     * 删除账户前校验是否已有流水，避免历史流水失去账户归属。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        Long userId = LoginUserContext.getUserId();
        findOwnedAccount(id, userId);
        Long transactionCount = transactionRecordMapper.selectCount(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getUserId, userId)
                .and(wrapper -> wrapper.eq(TransactionRecord::getAccountId, id).or().eq(TransactionRecord::getTargetAccountId, id)));
        if (transactionCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账户已有流水，第一版不允许删除");
        }
        accountMapper.delete(new LambdaQueryWrapper<Account>().eq(Account::getId, id).eq(Account::getUserId, userId));
    }

    /**
     * 查询当前用户拥有的账户，未命中时统一按不存在处理。
     */
    @Override
    public Account findOwnedAccount(Long id, Long userId) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getId, id)
                .eq(Account::getUserId, userId));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账户不存在");
        }
        return account;
    }

    /**
     * 按流水变化调整账户余额；delta 可正可负，调用方负责事务边界。
     */
    @Override
    public void adjustBalance(Long userId, Long accountId, BigDecimal delta) {
        Account account = findOwnedAccount(accountId, userId);
        account.setBalance(account.getBalance().add(delta));
        accountMapper.update(account, new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, accountId)
                .eq(Account::getUserId, userId));
    }

    /**
     * 转换为前端展示对象，隐藏持久化层内部字段。
     */
    private AccountVO toVO(Account account) {
        return AccountVO.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .initialBalance(account.getInitialBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .sortOrder(account.getSortOrder())
                .remark(account.getRemark())
                .build();
    }
}
