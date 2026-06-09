package com.xoassets.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xoassets.common.api.ErrorCode;
import com.xoassets.common.exception.BusinessException;
import com.xoassets.common.security.LoginUserContext;
import com.xoassets.module.budget.service.BudgetService;
import com.xoassets.module.budget.vo.BudgetSummaryVO;
import com.xoassets.module.dashboard.service.DashboardService;
import com.xoassets.module.dashboard.vo.DashboardOverviewVO;
import com.xoassets.module.report.dto.GenerateReportRequest;
import com.xoassets.module.report.service.AiReportService;
import com.xoassets.module.report.vo.AiReportVO;
import com.xoassets.persistence.entity.AiReport;
import com.xoassets.persistence.mapper.AiReportMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 财务报告服务实现：阶段七只生成模板化数据总结。
 */
@Service
public class AiReportServiceImpl implements AiReportService {

    /**
     * 日报类型常量。
     */
    private static final String TYPE_DAILY = "DAILY";
    /**
     * 周报类型常量。
     */
    private static final String TYPE_WEEKLY = "WEEKLY";
    /**
     * 月报类型常量。
     */
    private static final String TYPE_MONTHLY = "MONTHLY";

    /**
     * AI报告数据访问组件。
     */
    private final AiReportMapper aiReportMapper;
    /**
     * 首页服务。
     */
    private final DashboardService dashboardService;
    /**
     * 预算服务。
     */
    private final BudgetService budgetService;
    /**
     * JSON序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 注入业务依赖。
     */
    public AiReportServiceImpl(
            AiReportMapper aiReportMapper,
            DashboardService dashboardService,
            BudgetService budgetService,
            ObjectMapper objectMapper) {
        this.aiReportMapper = aiReportMapper;
        this.dashboardService = dashboardService;
        this.budgetService = budgetService;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前用户报告列表。
     */
    @Override
    public List<AiReportVO> list() {
        Long userId = LoginUserContext.getUserId();
        return aiReportMapper.selectList(new LambdaQueryWrapper<AiReport>()
                        .eq(AiReport::getUserId, userId)
                        .orderByDesc(AiReport::getReportDate)
                        .orderByDesc(AiReport::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 查询当前用户自己的报告详情。
     */
    @Override
    public AiReportVO detail(Long id) {
        Long userId = LoginUserContext.getUserId();
        AiReport report = aiReportMapper.selectOne(new LambdaQueryWrapper<AiReport>()
                .eq(AiReport::getId, id)
                .eq(AiReport::getUserId, userId));
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告不存在");
        }
        return toVO(report);
    }

    /**
     * 生成模板化报告预览，保存后返回；不调用真实大模型。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AiReportVO generatePreview(GenerateReportRequest request) {
        Long userId = LoginUserContext.getUserId();
        String reportType = normalizeType(request.getReportType());
        LocalDate reportDate = request.getReportDate() == null ? LocalDate.now() : request.getReportDate();
        YearMonth month = YearMonth.from(reportDate);
        DashboardOverviewVO overview = dashboardService.overview(month);
        BudgetSummaryVO budget = budgetService.summary(month.toString());
        AiReport report = new AiReport();
        report.setUserId(userId);
        report.setReportType(reportType);
        report.setReportDate(reportDate);
        report.setTitle(buildTitle(reportType, reportDate));
        report.setContent(buildContent(overview, budget));
        report.setSummaryJson(buildSummaryJson(overview, budget));
        report.setStatus("SUCCESS");
        report.setDeleted(0);
        aiReportMapper.insert(report);
        return toVO(report);
    }

    /**
     * 生成模板化文本，只做数据总结与风险提示，不提供投资买卖建议。
     */
    private String buildContent(DashboardOverviewVO overview, BudgetSummaryVO budget) {
        return """
                财务概览：
                当前总资产为 %s，净资产为 %s。本月收入 %s，本月支出 %s，今日支出 %s。

                预算进度：
                本月预算使用率为 %s%%，当前状态为 %s，剩余预算 %s。

                投资观察：
                当前投资总市值为 %s，投资浮动盈亏为 %s。该报告仅做数据总结和风险提示，不提供任何投资买入或卖出建议。

                复盘提示：
                请关注本月支出节奏、预算使用率和净资产变化，优先确认异常支出和即将超支的预算分类。
                """.formatted(
                overview.getTotalAssets(),
                overview.getNetAssets(),
                overview.getMonthlyIncome(),
                overview.getMonthlyExpense(),
                overview.getTodayExpense(),
                budget.getUsageRate(),
                budget.getUsageStatusLabel(),
                budget.getTotalRemaining(),
                overview.getInvestmentMarketValue(),
                overview.getInvestmentFloatingProfit());
    }

    /**
     * 保存结构化摘要，方便后续真实 AI 接入和前端扩展。
     */
    private String buildSummaryJson(DashboardOverviewVO overview, BudgetSummaryVO budget) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAssets", overview.getTotalAssets());
        summary.put("netAssets", overview.getNetAssets());
        summary.put("monthlyIncome", overview.getMonthlyIncome());
        summary.put("monthlyExpense", overview.getMonthlyExpense());
        summary.put("todayExpense", overview.getTodayExpense());
        summary.put("investmentMarketValue", overview.getInvestmentMarketValue());
        summary.put("investmentFloatingProfit", overview.getInvestmentFloatingProfit());
        summary.put("budgetUsageRate", budget.getUsageRate());
        summary.put("budgetStatus", budget.getUsageStatus());
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报告摘要生成失败");
        }
    }

    /**
     * 标准化报告类型。
     */
    private String normalizeType(String type) {
        String value = type == null ? TYPE_DAILY : type.toUpperCase();
        if (!TYPE_DAILY.equals(value) && !TYPE_WEEKLY.equals(value) && !TYPE_MONTHLY.equals(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报告类型只支持 DAILY、WEEKLY、MONTHLY");
        }
        return value;
    }

    /**
     * 生成报告标题。
     */
    private String buildTitle(String reportType, LocalDate reportDate) {
        String label = switch (reportType) {
            case TYPE_WEEKLY -> "周报";
            case TYPE_MONTHLY -> "月报";
            default -> "日报";
        };
        return reportDate + " 财务" + label;
    }

    /**
     * 转换报告展示对象。
     */
    private AiReportVO toVO(AiReport report) {
        return AiReportVO.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .reportDate(report.getReportDate())
                .title(report.getTitle())
                .content(report.getContent())
                .summaryJson(report.getSummaryJson())
                .status(report.getStatus())
                .statusLabel("SUCCESS".equals(report.getStatus()) ? "已生成" : "失败")
                .createdAt(report.getCreatedAt())
                .build();
    }
}
