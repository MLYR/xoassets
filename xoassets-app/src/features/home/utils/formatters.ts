export function formatMoney(value: number | null | undefined, currency = '¥') {
  // 后端返回 null 表示缺少有效口径，移动端展示 --，不能回填为 0。
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }

  return `${currency}${value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`;
}

export function formatSignedMoney(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }

  const prefix = value > 0 ? '+' : '';
  return `${prefix}${formatMoney(value)}`;
}

export function formatPercent(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }

  return `${value.toFixed(1)}%`;
}
