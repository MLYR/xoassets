package com.xoassets.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xoassets.persistence.entity.SnapshotRebuildTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产快照重建任务 Mapper。
 */
@Mapper
public interface SnapshotRebuildTaskMapper extends BaseMapper<SnapshotRebuildTask> {
}
