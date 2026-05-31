package com.xoassets.module.report.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 模板化报告生成请求参数。
 */
@Data
public class GenerateReportRequest {

    private String reportType = "DAILY";
    private LocalDate reportDate;
}
