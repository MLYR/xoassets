package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.AssetSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产快照 Mapper。
 */
@Mapper
public interface AssetSnapshotMapper extends BaseMapper<AssetSnapshot> {
}
