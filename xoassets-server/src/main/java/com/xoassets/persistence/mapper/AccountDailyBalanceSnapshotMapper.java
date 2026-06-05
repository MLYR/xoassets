package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.AccountDailyBalanceSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户日终余额快照 Mapper。
 */
@Mapper
public interface AccountDailyBalanceSnapshotMapper extends BaseMapper<AccountDailyBalanceSnapshot> {
}
