package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资产快照重建任务：补录历史流水时合并待处理日期范围，避免同步请求长时间阻塞。
 */
@Data
@TableName("xo_snapshot_rebuild_task")
public class SnapshotRebuildTask {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 需要重建的开始日期。
     */
    private LocalDate startDate;
    /**
     * 需要重建的结束日期。
     */
    private LocalDate endDate;
    /**
     * 任务状态：PENDING RUNNING SUCCESS FAILED。
     */
    private String status;
    /**
     * 触发来源。
     */
    private String triggerType;
    /**
     * 失败原因摘要。
     */
    private String errorMessage;
    /**
     * 重试次数。
     */
    private Integer retryCount;
    /**
     * 最后执行时间。
     */
    private LocalDateTime lastRunAt;
    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleted;
}
