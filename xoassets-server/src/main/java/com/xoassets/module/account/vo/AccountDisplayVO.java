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
    /**
     * 展示类型。
     */
    private String displayType;
    /**
     * 脱敏账号。
     */
    private String maskedNo;
    /**
     * 账户分组。
     */
    private String group;
    /**
     * 是否默认账户。
     */
    private Boolean isDefault;
    /**
     * 标签文案。
     */
    private String tagText;
    /**
     * 可用额度。
     */
    private BigDecimal availableCredit;
}
