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

    private Long id;
    private String name;
    private String type;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private String currency;
    private Integer status;
    private Integer sortOrder;
    private String remark;
}
