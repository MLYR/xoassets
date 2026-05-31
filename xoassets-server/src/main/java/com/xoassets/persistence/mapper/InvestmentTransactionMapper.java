package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.InvestmentTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投资交易 Mapper。
 */
@Mapper
public interface InvestmentTransactionMapper extends BaseMapper<InvestmentTransaction> {
}
