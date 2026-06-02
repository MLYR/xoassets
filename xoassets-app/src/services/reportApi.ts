/* AI 报告 API */
import { request } from './http'

export interface AiReportItem {
  id: string
  reportType: string
  reportDate: string
  title: string
  content: string
  summaryJson?: string | null
  status: string
  statusLabel: string
  createdAt: string
}

export const reportApi = {
  list() {
    return request<AiReportItem[]>({ url: '/reports', method: 'GET' })
  },
  detail(id: string) {
    return request<AiReportItem>({ url: `/reports/${id}`, method: 'GET' })
  }
}
