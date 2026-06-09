package com.xoassets.module.export.dto;

import com.xoassets.module.account.dto.AccountLedgerQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账户资金明细导出参数，accountId 必须由当前用户拥有。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountLedgerExportQuery extends AccountLedgerQuery {

    /**
     * 账户ID。
     */
    private Long accountId;
}
