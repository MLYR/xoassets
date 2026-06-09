package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户投资持仓实体，所有查询和修改必须按 user_id 隔离。
 */
@Data
@TableName("xo_holding")
public class Holding {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 资产ID。
     */
    private Long assetId;
    /**
     * 持仓数量。
     */
    private BigDecimal quantity;
    /**
     * 平均成本。
     */
    private BigDecimal avgCost;
    /**
     * 总成本。
     */
    private BigDecimal totalCost;
    /**
     * 备注。
     */
    private String remark;
    /**
     * 状态。
     */
    private Integer status;
    /**
     * 数据版本号。
     */
    private Long version;
    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;
}
