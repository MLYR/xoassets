package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.InvestmentDailySnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户投资资产日快照 Mapper。
 */
@Mapper
public interface InvestmentDailySnapshotMapper extends BaseMapper<InvestmentDailySnapshot> {
}
