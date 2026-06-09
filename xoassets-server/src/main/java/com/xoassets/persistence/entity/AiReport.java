package com.xoassets.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 财务报告实体，阶段七只保存模板化报告，不调用真实 AI。
 */
@Data
@TableName("xo_ai_report")
public class AiReport {

    /**
     * 主键ID。
     */
    private Long id;
    /**
     * 所属用户ID。
     */
    private Long userId;
    /**
     * 报告类型。
     */
    private String reportType;
    /**
     * 日期。
     */
    private LocalDate reportDate;
    /**
     * 标题。
     */
    private String title;
    /**
     * 内容。
     */
    private String content;
    /**
     * 摘要JSON。
     */
    private String summaryJson;
    /**
     * 状态。
     */
    private String status;
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
