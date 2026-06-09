package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 账户返回对象。
 */
@Data
@Builder
public class AccountVO {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 名称。
     */
    private String name;
    /**
     * 业务类型。
     */
    private String type;
    /**
     * 账户余额。
     */
    private BigDecimal balance;
    /**
     * 初始余额。
     */
    private BigDecimal initialBalance;
    /**
     * 币种。
     */
    private String currency;
    /**
     * 状态。
     */
    private Integer status;
    /**
     * 排序值。
     */
    private Integer sortOrder;
    /**
     * 备注。
     */
    private String remark;
}
