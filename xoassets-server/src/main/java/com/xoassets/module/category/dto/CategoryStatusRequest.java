package com.xoassets.module.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类启用状态修改请求参数。
 */
@Data
public class CategoryStatusRequest {

    /**
     * 状态。
     */
    @NotNull(message = "分类状态不能为空")
    private Integer status;
}
