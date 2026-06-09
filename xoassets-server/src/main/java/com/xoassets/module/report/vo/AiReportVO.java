package com.xoassets.module.report.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * AI 财务报告展示对象。
 */
@Data
@Builder
public class AiReportVO {

    /**
     * 主键ID。
     */
    private Long id;
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
     * 状态文案。
     */
    private String statusLabel;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
