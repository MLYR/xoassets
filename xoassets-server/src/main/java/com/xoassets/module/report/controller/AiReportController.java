package com.xoassets.module.report.controller;

import com.xoassets.common.api.Result;
import com.xoassets.module.report.dto.GenerateReportRequest;
import com.xoassets.module.report.service.AiReportService;
import com.xoassets.module.report.vo.AiReportVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 财务报告接口。
 */
@RestController
@RequestMapping("/api/reports")
public class AiReportController {

    /**
     * AI报告服务。
     */
    private final AiReportService aiReportService;

    /**
     * 注入接口依赖。
     */
    public AiReportController(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    /**
     * 查询当前用户报告列表。
     */
    @GetMapping
    public Result<List<AiReportVO>> list() {
        return Result.success(aiReportService.list());
    }

    /**
     * 查询当前用户报告详情。
     */
    @GetMapping("/{id}")
    public Result<AiReportVO> detail(@PathVariable Long id) {
        return Result.success(aiReportService.detail(id));
    }

    /**
     * 生成模板化报告，不调用真实 AI。
     */
    @PostMapping("/generate-preview")
    public Result<AiReportVO> generatePreview(@RequestBody GenerateReportRequest request) {
        return Result.success(aiReportService.generatePreview(request));
    }
}
