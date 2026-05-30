// 金融展示常量：金额、趋势和状态色统一从这里读取。
export const CURRENCY_SYMBOL = '¥';

export const financeColors = {
  income: '#10b981',
  expense: '#ef4444',
  neutral: '#64748b',
  warning: '#f59e0b',
  primary: '#3b82f6'
} as const;

export const transactionTypes = ['收入', '支出'] as const;

export const statusMap = {
  已完成: 'success',
  进行中: 'warning',
  待确认: 'warning',
  已超支: 'danger',
  正常: 'success',
  关注: 'warning',
  达成: 'success'
} as const;
