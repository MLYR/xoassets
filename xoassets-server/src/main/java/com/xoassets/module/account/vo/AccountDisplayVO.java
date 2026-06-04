package com.xoassets.module.account.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 移动端账户页展示对象，在 AccountVO 基础上补充 UI 分组和标签字段。
 */
@Data
@Builder
public class AccountDisplayVO {

    private Long id;
    private String name;
    private String type;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private String currency;
    private Integer status;
    private Integer sortOrder;
    private String remark;
    private String displayType;
    private String maskedNo;
    private String group;
    private Boolean isDefault;
    private String tagText;
    private BigDecimal availableCredit;
}
