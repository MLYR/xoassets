package com.xoassets.module.report.service;

import com.xoassets.module.report.dto.GenerateReportRequest;
import com.xoassets.module.report.vo.AiReportVO;
import java.util.List;

/**
 * AI 财务报告服务接口。
 */
public interface AiReportService {

    /**
     * 查询当前用户报告列表。
     */
    List<AiReportVO> list();

    /**
     * 查询当前用户报告详情。
     */
    AiReportVO detail(Long id);

    /**
     * 生成模板化报告预览并保存。
     */
    AiReportVO generatePreview(GenerateReportRequest request);
}
