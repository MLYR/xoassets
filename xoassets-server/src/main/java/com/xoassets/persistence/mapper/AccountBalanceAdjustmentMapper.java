package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.AccountBalanceAdjustment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户余额修正 Mapper。
 */
@Mapper
public interface AccountBalanceAdjustmentMapper extends BaseMapper<AccountBalanceAdjustment> {
}
