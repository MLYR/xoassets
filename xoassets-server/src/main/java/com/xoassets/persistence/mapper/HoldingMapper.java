package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.Holding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投资持仓 Mapper。
 */
@Mapper
public interface HoldingMapper extends BaseMapper<Holding> {
}
