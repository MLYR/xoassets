package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公共资产基础实体，不归属单个用户。
 */
@Data
@TableName("xo_asset")
public class Asset {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 资产代码。
     */
    private String symbol;
    /**
     * 名称。
     */
    private String name;
    /**
     * 业务类型。
     */
    private String type;
    /**
     * 交易市场。
     */
    private String market;
    /**
     * 币种。
     */
    private String currency;
    /**
     * 行情来源。
     */
    private String quoteSource;
    /**
     * 行情查询键。
     */
    private String quoteKey;
    /**
     * 状态。
     */
    private Integer status;
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
