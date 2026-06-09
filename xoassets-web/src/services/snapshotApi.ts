// 资产快照 API：用于首页净资产变化和分析页资产趋势。
import { request } from './http';

/** 资产快照项。 */
export interface AssetSnapshotItem {
  /** ID。 */
  id: string;
  /** 快照日期。 */
  snapshotDate: string;
  /** 账户资产。 */
  cashAsset: number;
  /** 投资资产。 */
  investmentAsset: number;
  /** 总资产。 */
  totalAsset: number;
  /** 负债。 */
  liability: number;
  /** 净资产。 */
  netAsset: number;
  /** 投资成本。 */
  investmentCost: number;
  /** 投资收益。 */
  investmentProfit: number;
  /** 投资收益率。 */
  investmentProfitRate: number;
  /** 当月收入。 */
  monthlyIncome: number;
  /** 当月支出。 */
  monthlyExpense: number;
  /** 当月结余。 */
  monthlyBalance: number;
  /** 预算已用金额。 */
  budgetUsedAmount: number;
  /** 预算总额。 */
  budgetTotalAmount: number;
  /** 预算使用率。 */
  budgetUsageRate: number;
}

/** 最新快照返回数据。 */
export interface AssetSnapshotLatest {
  /** 最新快照。 */
  latest: AssetSnapshotItem | null;
  /** 较昨日净资产变化。 */
  netAssetChangeFromYesterday: number | null;
  /** 较月初净资产变化。 */
  netAssetChangeFromMonthStart: number | null;
}

export const snapshotApi = {
  // 查询当前用户最新快照，后端同时返回昨日和本月初变化。
  latest() {
    return request<AssetSnapshotLatest>({
      url: '/snapshots/latest',
      method: 'GET'
    });
  },
  // 查询快照趋势；不传日期时后端默认最近 30 天。
  trend(params?: { startDate?: string; endDate?: string }) {
    return request<AssetSnapshotItem[]>({
      url: '/snapshots/trend',
      method: 'GET',
      params
    });
  },
  // 手动生成今日快照，主要用于本地验收和用户主动刷新。
  generateToday() {
    return request<AssetSnapshotItem>({
      url: '/snapshots/generate-today',
      method: 'POST'
    });
  },
  // 手动重建指定日期快照，用于本地对账和历史数据修复。
  generate(params?: { snapshotDate?: string }) {
    return request<AssetSnapshotItem>({
      url: '/snapshots/generate',
      method: 'POST',
      params
    });
  }
};
