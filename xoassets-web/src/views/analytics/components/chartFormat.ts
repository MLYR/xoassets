// 分析页图表格式工具：统一金额 tooltip、坐标轴格式和暗色主题文本色。
import { formatAmount } from '@/utils/format';

export function chartColor(name: string, fallback = '') {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

export function amountTooltip(value: unknown) {
  const amount = Number(value);
  return Number.isFinite(amount) ? formatAmount(amount) : '--';
}

export function amountAxis(value: number) {
  if (!Number.isFinite(Number(value))) {
    return '';
  }
  const amount = Number(value);
  const abs = Math.abs(amount);
  if (abs >= 100000000) {
    return `¥${(amount / 100000000).toFixed(1)}亿`;
  }
  if (abs >= 10000) {
    return `¥${(amount / 10000).toFixed(1)}万`;
  }
  return `¥${amount.toFixed(0)}`;
}

export function categoryAxis(data: string[]) {
  const axisText = chartColor('--xo-muted');
  const axisLine = chartColor('--xo-border-strong');
  return {
    type: 'category' as const,
    data,
    axisLabel: { color: axisText },
    axisLine: { lineStyle: { color: axisLine } }
  };
}

export function valueAxis() {
  const axisText = chartColor('--xo-muted');
  const splitLine = chartColor('--xo-border');
  return {
    type: 'value' as const,
    axisLabel: { color: axisText, formatter: amountAxis },
    splitLine: { lineStyle: { color: splitLine } }
  };
}

export function chartLegend(top: number | string = 0) {
  return {
    top,
    textStyle: { color: chartColor('--xo-muted') }
  };
}
