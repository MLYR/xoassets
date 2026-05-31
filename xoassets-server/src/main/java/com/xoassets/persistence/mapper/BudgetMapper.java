package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预算表 Mapper。
 */
@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}
