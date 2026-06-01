// 资产快照 API：用于首页净资产变化和分析页资产趋势。
import { request } from './http';

export interface AssetSnapshotItem {
  id: string;
  snapshotDate: string;
  cashAsset: number;
  investmentAsset: number;
  totalAsset: number;
  liability: number;
  netAsset: number;
  investmentCost: number;
  investmentProfit: number;
  investmentProfitRate: number;
  monthlyIncome: number;
  monthlyExpense: number;
  monthlyBalance: number;
  budgetUsedAmount: number;
  budgetTotalAmount: number;
  budgetUsageRate: number;
}

export interface AssetSnapshotLatest {
  latest: AssetSnapshotItem | null;
  netAssetChangeFromYesterday: number;
  netAssetChangeFromMonthStart: number;
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
  }
};
