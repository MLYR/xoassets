package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.Asset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公共资产 Mapper。
 */
@Mapper
public interface AssetMapper extends BaseMapper<Asset> {
}
