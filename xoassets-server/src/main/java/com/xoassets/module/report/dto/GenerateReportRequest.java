package com.xoassets.module.report.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 模板化报告生成请求参数。
 */
@Data
public class GenerateReportRequest {

    /**
     * 报告类型。
     */
    private String reportType = "DAILY";
    /**
     * 日期。
     */
    private LocalDate reportDate;
}
