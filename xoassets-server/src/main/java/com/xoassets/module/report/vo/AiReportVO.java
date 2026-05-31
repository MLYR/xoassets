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

    private Long id;
    private String reportType;
    private LocalDate reportDate;
    private String title;
    private String content;
    private String summaryJson;
    private String status;
    private String statusLabel;
    private LocalDateTime createdAt;
}
