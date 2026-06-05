package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.MarketCalendar;
import org.apache.ibatis.annotations.Mapper;

/**
 * 市场交易日历 Mapper。
 */
@Mapper
public interface MarketCalendarMapper extends BaseMapper<MarketCalendar> {
}
