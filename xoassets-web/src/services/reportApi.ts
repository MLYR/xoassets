// AI 报告 API：阶段七只生成模板化财务报告，不调用真实 AI。
import { request } from './http';

/** 报告类型。 */
export type ReportType = 'DAILY' | 'WEEKLY' | 'MONTHLY';

/** 报告列表项。 */
export interface AiReportItem {
  /** ID。 */
  id: string;
  /** 报告类型。 */
  reportType: ReportType;
  /** 报告日期。 */
  reportDate: string;
  /** 标题。 */
  title: string;
  /** 内容。 */
  content: string;
  /** 摘要JSON。 */
  summaryJson?: string | null;
  /** 状态。 */
  status: string;
  /** 状态文案。 */
  statusLabel: string;
  /** 创建时间。 */
  createdAt: string;
}

/** 报告生成参数。 */
export interface GenerateReportRequest {
  /** 报告类型。 */
  reportType: ReportType;
  /** 报告日期。 */
  reportDate?: string;
}

export const reportApi = {
  // 查询列表。
  list() {
    return request<AiReportItem[]>({
      url: '/reports',
      method: 'GET'
    });
  },
  // 查询详情。
  detail(id: string) {
    return request<AiReportItem>({
      url: `/reports/${id}`,
      method: 'GET'
    });
  },
  // 生成报告预览。
  generatePreview(data: GenerateReportRequest) {
    return request<AiReportItem>({
      url: '/reports/generate-preview',
      method: 'POST',
      data
    });
  }
};
