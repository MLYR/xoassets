// AI 报告 API：阶段七只生成模板化财务报告，不调用真实 AI。
import { request } from './http';

export type ReportType = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface AiReportItem {
  id: string;
  reportType: ReportType;
  reportDate: string;
  title: string;
  content: string;
  summaryJson?: string | null;
  status: string;
  statusLabel: string;
  createdAt: string;
}

export interface GenerateReportRequest {
  reportType: ReportType;
  reportDate?: string;
}

export const reportApi = {
  list() {
    return request<AiReportItem[]>({
      url: '/reports',
      method: 'GET'
    });
  },
  detail(id: string) {
    return request<AiReportItem>({
      url: `/reports/${id}`,
      method: 'GET'
    });
  },
  generatePreview(data: GenerateReportRequest) {
    return request<AiReportItem>({
      url: '/reports/generate-preview',
      method: 'POST',
      data
    });
  }
};
