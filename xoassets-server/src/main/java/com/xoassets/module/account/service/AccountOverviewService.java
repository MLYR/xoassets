package com.xoassets.module.account.service;

import com.xoassets.module.account.vo.AccountOverviewVO;

/**
 * 账户页聚合服务，提供移动端账户总览所需的展示口径。
 */
public interface AccountOverviewService {

    /**
     * 查询当前登录用户的账户总览。
     */
    AccountOverviewVO overview();
}
