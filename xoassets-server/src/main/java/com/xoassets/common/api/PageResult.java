package com.xoassets.common.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页返回结构，屏蔽 MyBatis-Plus Page 的内部字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 分页记录。
     */
    private List<T> records;
    /**
     * 总条数。
     */
    private long total;
    /**
     * 页码。
     */
    private long pageNo;
    /**
     * 每页条数。
     */
    private long pageSize;
}
