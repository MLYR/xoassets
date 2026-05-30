// 格式化工具：金额和百分比统一从这里输出，避免页面各自实现。
import { CURRENCY_SYMBOL } from '@/constants/finance';

// 格式化金额，withSign 为 true 时正数也显示加号。
export function formatAmount(value: number, withSign = false): string {
  const sign = withSign && value > 0 ? '+' : value < 0 ? '-' : '';
  const amount = Math.abs(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });

  return `${sign}${CURRENCY_SYMBOL} ${amount}`;
}

// 格式化百分比，正数显示加号，负数保留自身符号。
export function formatPercent(value: number): string {
  const sign = value > 0 ? '+' : '';
  return `${sign}${value.toFixed(1)}%`;
}

// 计算进度百分比，超过目标时最高显示 100。
export function progressPercent(current: number, target: number): number {
  if (target <= 0) {
    return 0;
  }

  return Math.min(100, Math.round((current / target) * 100));
}
