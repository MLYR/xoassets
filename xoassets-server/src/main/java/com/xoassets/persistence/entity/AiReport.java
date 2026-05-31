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

    private Long id;
    private Long userId;
    private String reportType;
    private LocalDate reportDate;
    private String title;
    private String content;
    private String summaryJson;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
