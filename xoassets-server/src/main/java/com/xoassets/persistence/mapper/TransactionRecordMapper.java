package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.TransactionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流水 Mapper。
 */
@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {
}
