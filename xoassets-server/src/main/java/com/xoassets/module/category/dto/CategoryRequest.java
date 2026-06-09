package com.xoassets.module.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类新增和修改请求参数。
 */
@Data
public class CategoryRequest {

    /**
     * 名称。
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 业务类型。
     */
    @NotBlank(message = "分类类型不能为空")
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
    private Integer status = 1;
    /**
     * 排序值。
     */
    private Integer sortOrder = 0;
}
