// 业务类型定义：当前页面和 mock 服务共享这些结构。
// 流水方向类型，当前前端只展示收入和支出。
export type TransactionType = '收入' | '支出';

// 页面通用状态文案。
export type StatusText = '已完成' | '进行中' | '待确认' | '已超支' | '正常' | '关注' | '达成';

// 流水列表项。
export interface Transaction {
  id: number;
  date: string;
  type: TransactionType;
  category: string;
  account: string;
  amount: number;
  balance?: number;
  note: string;
  status: StatusText;
}

// 首页指标卡数据。
export interface Metric {
  title: string;
  value: number;
  trend: number;
  description: string;
  tone?: 'success' | 'danger' | 'warning' | 'primary';
}

// 账户列表项。
export interface Account {
  id: number;
  name: string;
  type: string;
  balance: number;
  status: StatusText;
  updatedAt: string;
}

// 投资持仓列表项。
export interface Holding {
  id: number;
  name: string;
  code: string;
  marketValue: number;
  profit: number;
  profitRate: number;
  allocation: number;
}

// 预算列表项。
export interface Budget {
  id: number;
  category: string;
  used: number;
  limit: number;
  status: StatusText;
}

// 资产目标列表项。
export interface Goal {
  id: number;
  name: string;
  target: number;
  current: number;
  deadline: string;
  status: StatusText;
}

// AI 报告列表项。
export interface Report {
  id: number;
  title: string;
  summary: string;
  status: StatusText;
  createdAt: string;
}

// ECharts 图表通用点位结构。
export interface ChartPoint {
  name: string;
  value: number;
}
