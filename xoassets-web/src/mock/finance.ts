// Mock 数据：第一版前端不接后端，所有页面从这里读取稳定样例。
import type { Account, Budget, ChartPoint, Goal, Holding, Metric, Report, Transaction } from '@/types/finance';

export const dashboardMetrics: Metric[] = [
  { title: '总资产', value: 254180, trend: 2.5, description: '较上月', tone: 'success' },
  { title: '本月收入', value: 137850, trend: 8.2, description: '较上月', tone: 'success' },
  { title: '本月支出', value: 129530, trend: -5.8, description: '较上月', tone: 'danger' },
  { title: '本月结余', value: 8320, trend: 12.5, description: '较上月', tone: 'primary' }
];

export const assetTrend: ChartPoint[] = [
  { name: '01/01', value: 235000 },
  { name: '01/08', value: 238000 },
  { name: '01/15', value: 241000 },
  { name: '01/22', value: 244000 },
  { name: '01/29', value: 246000 },
  { name: '02/05', value: 250000 },
  { name: '02/12', value: 254180 }
];

export const expenseBreakdown: ChartPoint[] = [
  { name: '餐饮', value: 3580 },
  { name: '购物', value: 2860 },
  { name: '交通', value: 1240 },
  { name: '娱乐', value: 980 },
  { name: '其他', value: 650 }
];

export const monthlyBalance: ChartPoint[] = [
  { name: '8月', value: 6200 },
  { name: '9月', value: 7400 },
  { name: '10月', value: 6900 },
  { name: '11月', value: 8200 },
  { name: '12月', value: 7900 },
  { name: '1月', value: 8320 }
];

export const transactions: Transaction[] = [
  { id: 1, date: '2026-05-29 14:30', type: '支出', category: '餐饮', account: '招商银行信用卡', amount: -128.5, balance: 254180, note: '午餐', status: '已完成' },
  { id: 2, date: '2026-05-29 09:00', type: '收入', category: '工资', account: '工商银行储蓄卡', amount: 15000, balance: 254308.5, note: '5月工资', status: '已完成' },
  { id: 3, date: '2026-05-28 20:15', type: '支出', category: '购物', account: '支付宝', amount: -599, balance: 239308.5, note: '运动鞋', status: '已完成' },
  { id: 4, date: '2026-05-28 18:30', type: '支出', category: '交通', account: '微信', amount: -42, balance: 239907.5, note: '打车', status: '已完成' },
  { id: 5, date: '2026-05-27 16:20', type: '收入', category: '理财收益', account: '支付宝', amount: 285.3, balance: 239949.5, note: '货币基金收益', status: '已完成' },
  { id: 6, date: '2026-05-27 12:45', type: '支出', category: '餐饮', account: '招商银行信用卡', amount: -89, balance: 239664.2, note: '咖啡店', status: '已完成' },
  { id: 7, date: '2026-05-26 19:00', type: '支出', category: '娱乐', account: '微信', amount: -120, balance: 239753.2, note: '电影票', status: '已完成' },
  { id: 8, date: '2026-05-26 14:20', type: '支出', category: '交通', account: '微信', amount: -5.5, balance: 239873.2, note: '地铁', status: '已完成' }
];

export const accounts: Account[] = [
  { id: 1, name: '工商银行储蓄卡', type: '储蓄卡', balance: 128650.2, status: '正常', updatedAt: '2026-05-30' },
  { id: 2, name: '招商银行信用卡', type: '信用卡', balance: -5380.6, status: '关注', updatedAt: '2026-05-29' },
  { id: 3, name: '支付宝', type: '电子钱包', balance: 38920.8, status: '正常', updatedAt: '2026-05-29' },
  { id: 4, name: '微信', type: '电子钱包', balance: 12680.5, status: '正常', updatedAt: '2026-05-28' },
  { id: 5, name: '证券账户', type: '投资账户', balance: 79280.1, status: '正常', updatedAt: '2026-05-30' }
];

export const holdings: Holding[] = [
  { id: 1, name: '沪深300ETF', code: '510300', marketValue: 38500, profit: 3280, profitRate: 9.3, allocation: 42 },
  { id: 2, name: '纳指100ETF', code: '513100', marketValue: 21680, profit: 1860, profitRate: 9.4, allocation: 24 },
  { id: 3, name: '短债基金', code: 'A10021', marketValue: 12600, profit: 210, profitRate: 1.7, allocation: 14 },
  { id: 4, name: '银行理财', code: 'R2026', marketValue: 18500, profit: -320, profitRate: -1.7, allocation: 20 }
];

export const budgets: Budget[] = [
  { id: 1, category: '餐饮', used: 3580, limit: 4200, status: '正常' },
  { id: 2, category: '购物', used: 2860, limit: 3000, status: '关注' },
  { id: 3, category: '交通', used: 1240, limit: 1800, status: '正常' },
  { id: 4, category: '娱乐', used: 980, limit: 900, status: '已超支' }
];

export const goals: Goal[] = [
  { id: 1, name: '应急备用金', target: 60000, current: 48200, deadline: '2026-12-31', status: '进行中' },
  { id: 2, name: '年度投资本金', target: 120000, current: 79280, deadline: '2026-11-30', status: '进行中' },
  { id: 3, name: '旅行基金', target: 18000, current: 18000, deadline: '2026-08-01', status: '达成' }
];

export const reports: Report[] = [
  { id: 1, title: '5月资产复盘', summary: '净资产稳步增长，支出集中在餐饮和购物，建议关注信用卡账单节奏。', status: '已完成', createdAt: '2026-05-30' },
  { id: 2, title: '预算健康检查', summary: '娱乐预算已超支，购物预算接近上限，建议下周降低非必要消费。', status: '待确认', createdAt: '2026-05-29' },
  { id: 3, title: '投资组合观察', summary: '权益资产占比适中，短债基金波动低，可继续作为现金管理补充。', status: '已完成', createdAt: '2026-05-28' }
];
