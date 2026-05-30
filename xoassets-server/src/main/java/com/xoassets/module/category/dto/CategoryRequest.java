package com.xoassets.module.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类新增和修改请求参数。
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    @NotBlank(message = "分类类型不能为空")
    private String type;

    private String icon;
    private String color;
    private Integer status = 1;
    private Integer sortOrder = 0;
}
