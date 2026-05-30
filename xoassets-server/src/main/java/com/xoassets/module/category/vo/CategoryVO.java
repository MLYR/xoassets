package com.xoassets.module.category.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 分类返回对象。
 */
@Data
@Builder
public class CategoryVO {

    private Long id;
    private String name;
    private String type;
    private String icon;
    private String color;
    private Integer status;
    private Integer sortOrder;
}
