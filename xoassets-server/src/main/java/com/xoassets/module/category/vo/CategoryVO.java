package com.xoassets.module.category.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 分类返回对象。
 */
@Data
@Builder
public class CategoryVO {

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
     * 图标。
     */
    private String icon;
    /**
     * 颜色。
     */
    private String color;
    /**
     * 状态。
     */
    private Integer status;
    /**
     * 排序值。
     */
    private Integer sortOrder;
}
