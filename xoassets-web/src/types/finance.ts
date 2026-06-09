// 业务类型定义：当前页面和 mock 服务共享这些结构。
// 流水方向类型，当前前端只展示收入和支出。
export type TransactionType = '收入' | '支出';

// 页面通用状态文案。
export type StatusText = '已完成' | '进行中' | '待确认' | '已超支' | '正常' | '关注' | '达成';

// 流水列表项。
export interface Transaction {
  /** ID。 */
  id: number;
  /** 日期。 */
  date: string;
  /** 类型。 */
  type: TransactionType;
  /** 分类。 */
  category: string;
  /** 账户。 */
  account: string;
  /** 金额。 */
  amount: number;
  /** 余额。 */
  balance?: number;
  /** 备注。 */
  note: string;
  /** 状态。 */
  status: StatusText;
}

// 首页指标卡数据。
export interface Metric {
  /** 标题。 */
  title: string;
  /** 数值。 */
  value: number;
  /** 趋势值。 */
  trend: number;
  /** 说明。 */
  description: string;
  /** 颜色语义。 */
  tone?: 'success' | 'danger' | 'warning' | 'primary';
}

// 账户列表项。
export interface Account {
  /** ID。 */
  id: number;
  /** 名称。 */
  name: string;
  /** 类型。 */
  type: string;
  /** 余额。 */
  balance: number;
  /** 状态。 */
  status: StatusText;
  /** 更新时间。 */
  updatedAt: string;
}

// 投资持仓列表项。
export interface Holding {
  /** ID。 */
  id: number;
  /** 名称。 */
  name: string;
  /** 编码。 */
  code: string;
  /** 市值。 */
  marketValue: number;
  /** 收益。 */
  profit: number;
  /** 收益率。 */
  profitRate: number;
  /** 占比。 */
  allocation: number;
}

// 预算列表项。
export interface Budget {
  /** ID。 */
  id: number;
  /** 分类。 */
  category: string;
  /** 已用金额。 */
  used: number;
  /** 额度。 */
  limit: number;
  /** 状态。 */
  status: StatusText;
}

// 资产目标列表项。
export interface Goal {
  /** ID。 */
  id: number;
  /** 名称。 */
  name: string;
  /** 目标金额。 */
  target: number;
  /** 当前金额。 */
  current: number;
  /** 截止日期。 */
  deadline: string;
  /** 状态。 */
  status: StatusText;
}

// AI 报告列表项。
export interface Report {
  /** ID。 */
  id: number;
  /** 标题。 */
  title: string;
  /** 摘要。 */
  summary: string;
  /** 状态。 */
  status: StatusText;
  /** 创建时间。 */
  createdAt: string;
}

// ECharts 图表通用点位结构。
export interface ChartPoint {
  /** 名称。 */
  name: string;
  /** 数值。 */
  value: number;
}
