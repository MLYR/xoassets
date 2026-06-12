// 投资 API：封装资产、持仓、投资交易和手动报价接口。
import { request } from './http';

/** 资产类型。 */
export type AssetType = 'STOCK' | 'FUND' | 'CRYPTO' | 'OTHER';
/** 行情来源类型。 */
export type QuoteSource = 'MANUAL' | 'COINGECKO' | 'EASTMONEY' | 'SINA' | 'YAHOO' | 'ALPHA_VANTAGE' | 'TUSHARE' | 'AKSHARE';
/** 投资交易类型。 */
export type InvestmentTransactionType = 'BUY' | 'SELL';
/** 投资录入模式。 */
export type InvestmentInputMode = 'QUANTITY_PRICE' | 'AMOUNT_NAV';
/** 投资交易状态。 */
export type InvestmentTransactionStatus = 'NORMAL' | 'CONFIRMED' | 'PENDING_CONFIRM' | 'FAILED' | 'CANCELLED' | 'REVOKED';

/** 资产列表项。 */
export interface AssetItem {
  /** ID。 */
  id: string;
  /** 代码。 */
  symbol: string;
  /** 名称。 */
  name: string;
  /** 类型。 */
  type: AssetType;
  /** 市场。 */
  market?: string | null;
  /** 币种。 */
  currency: string;
  /** 行情来源。 */
  quoteSource: QuoteSource;
  /** 行情键。 */
  quoteKey?: string | null;
}

/** 资产识别结果。 */
export interface AssetLookupItem {
  /** 名称。 */
  name: string;
  /** 代码。 */
  symbol: string;
  /** 资产类型。 */
  assetType: AssetType;
  /** 市场。 */
  market?: string | null;
  /** 币种。 */
  currency: string;
  /** 行情来源。 */
  quoteSource: QuoteSource;
  /** 行情键。 */
  quoteKey: string;
  /** 最新价。 */
  latestPrice?: number | null;
  /** 昨收价。 */
  previousClose?: number | null;
  /** 涨跌幅。 */
  changePercent?: number | null;
  /** 报价时间。 */
  quoteTime?: string | null;
}

/** 资产保存参数。 */
export interface AssetRequest {
  /** 代码。 */
  symbol: string;
  /** 名称。 */
  name: string;
  /** 类型。 */
  type: AssetType;
  /** 市场。 */
  market?: string;
  /** 币种。 */
  currency: string;
  /** 行情来源。 */
  quoteSource: QuoteSource;
  /** 行情键。 */
  quoteKey?: string;
}

/** 持仓列表项。 */
export interface HoldingItem {
  /** ID。 */
  id: string;
  /** 资产ID。 */
  assetId: string;
  /** 资产名称。 */
  assetName: string | null;
  /** 代码。 */
  symbol: string | null;
  /** 资产类型。 */
  assetType: AssetType | null;
  /** 资产子类型。 */
  assetSubType?: string | null;
  /** 收益展示模式。 */
  profitDisplayMode?: string | null;
  /** 估值模式。 */
  valuationMode?: string | null;
  /** 交易场所。 */
  tradeVenue?: string | null;
  /** 市场。 */
  market?: string | null;
  /** 行情来源。 */
  quoteSource: QuoteSource | null;
  /** 币种。 */
  currency: string | null;
  /** 数量。 */
  quantity: number;
  /** 平均成本。 */
  avgCost: number;
  /** 总成本。 */
  totalCost: number;
  /** 最新价。 */
  latestPrice: number;
  /** 上一交易日价格。 */
  previousPrice?: number | null;
  /** 上上交易日价格。 */
  beforePreviousPrice?: number | null;
  /** 价格小数位。 */
  priceScale?: number | null;
  /** 最新价格时间。 */
  latestPriceTime?: string | null;
  /** 上一价格时间。 */
  previousPriceTime?: string | null;
  /** 价格日期。 */
  priceDate?: string | null;
  /** 今日价格是否可用。 */
  todayPriceAvailable?: boolean | null;
  /** 今日收益是否可用。 */
  todayProfitAvailable?: boolean | null;
  /** 价格状态。 */
  priceStatus?: string | null;
  /** 价格来源。 */
  latestPriceSource?: string | null;
  /** 市场状态。 */
  marketStatus?: string | null;
  /** 主收益名称。 */
  primaryProfitLabel?: string | null;
  /** 主收益金额。 */
  primaryProfitAmount?: number | null;
  /** 副收益名称。 */
  secondaryProfitLabel?: string | null;
  /** 副收益金额。 */
  secondaryProfitAmount?: number | null;
  /** 价格文案。 */
  priceLabel?: string | null;
  /** 市值。 */
  marketValue: number;
  /** 今日收益。 */
  todayProfit?: number | null;
  /** 今日收益率。 */
  todayProfitRate?: number | null;
  /** 按当前份额计算的今日收益。 */
  todayProfitByCurrentQuantity?: number | null;
  /** 按当前份额计算的今日收益率。 */
  todayProfitRateByCurrentQuantity?: number | null;
  /** 按上一日份额计算的今日收益。 */
  todayProfitByPreviousSnapshotQuantity?: number | null;
  /** 按上一日份额计算的今日收益率。 */
  todayProfitRateByPreviousSnapshotQuantity?: number | null;
  /** 今日涨跌幅。 */
  todayChangeRate?: number | null;
  /** 昨日收益。 */
  yesterdayProfit?: number | null;
  /** 昨日涨跌幅。 */
  yesterdayChangeRate?: number | null;
  /** 浮动盈亏。 */
  floatingProfit: number;
  /** 已实现收益。 */
  realizedProfit?: number | null;
  /** 总收益。 */
  totalProfit?: number | null;
  /** 总收益率。 */
  totalProfitRate?: number | null;
  /** 浮动盈亏率。 */
  floatingProfitRate: number;
  /** 回本涨跌幅。 */
  breakEvenRate?: number | null;
  /** 备注。 */
  remark?: string | null;
  /** 状态。 */
  status: number;
}

/** 持仓汇总。 */
export interface HoldingSummary {
  /** 持仓总市值。 */
  totalMarketValue: number;
  /** 总成本。 */
  totalCost: number;
  /** 今日收益是否可用。 */
  todayProfitAvailable?: boolean | null;
  /** 今日收益。 */
  todayProfit?: number | null;
  /** 今日收益率。 */
  todayProfitRate?: number | null;
  /** 昨日收益。 */
  yesterdayProfit?: number | null;
  /** 昨日收益率。 */
  yesterdayProfitRate?: number | null;
  /** 上月以来收益。 */
  lastMonthProfit?: number | null;
  /** 上月以来收益率。 */
  lastMonthProfitRate?: number | null;
  /** 浮动盈亏。 */
  floatingProfit: number;
  /** 浮动盈亏率。 */
  floatingProfitRate: number;
  /** 持仓数量。 */
  holdingCount: number;
}

/** 持仓详情汇总。 */
export interface HoldingDetailSummary {
  /** 买入总额。 */
  totalBuyAmount: number;
  /** 卖出总额。 */
  totalSellAmount: number;
  /** 手续费合计。 */
  totalFee: number;
  /** 待确认金额。 */
  pendingConfirmAmount?: number | null;
  /** 已实现收益。 */
  realizedProfit: number;
  /** 浮动盈亏。 */
  floatingProfit: number;
  /** 总收益。 */
  totalProfit: number;
  /** 总收益率。 */
  totalProfitRate: number;
  /** 买入次数。 */
  buyCount: number;
  /** 卖出次数。 */
  sellCount: number;
  /** 首次买入时间。 */
  firstBuyTime?: string | null;
  /** 最近交易时间。 */
  lastTradeTime?: string | null;
}

/** 资产价格项。 */
export interface AssetPriceItem {
  /** ID。 */
  id?: string;
  /** 资产ID。 */
  assetId?: string;
  /** 价格。 */
  price: number;
  /** 币种。 */
  currency: string;
  /** 昨收价。 */
  previousClose?: number | null;
  /** 涨跌额。 */
  changeAmount?: number | null;
  /** 涨跌幅。 */
  changePercent?: number | null;
  /** 来源。 */
  source: string;
  /** 报价时间。 */
  quoteTime: string;
  /** 市场状态。 */
  marketStatus?: string | null;
}

/** 持仓图表点。 */
export interface HoldingChartPoint {
  /** 报价时间。 */
  quoteTime: string;
  /** 总资产金额。 */
  totalAssetAmount: number;
  /** 总收益金额。 */
  totalProfitAmount: number;
}

/** 收益日历日期项。 */
export interface InvestmentCalendarDayProfit {
  /** 日期。 */
  date: string;
  /** 收益金额。 */
  profitAmount?: number | null;
  /** 收益率。 */
  profitRate?: number | null;
  /** 市值。 */
  marketValue?: number | null;
  /** 价格。 */
  price?: number | null;
  /** 上一交易日价格。 */
  previousPrice?: number | null;
  /** 是否有价格。 */
  hasPrice: boolean;
  /** 是否交易日。 */
  tradingDay?: boolean | null;
  /** 是否休市。 */
  marketClosed?: boolean | null;
  /** 状态文案。 */
  statusLabel?: string | null;
  /** 价格文案。 */
  priceLabel?: string | null;
}

/** 持仓详情数据。 */
export interface HoldingDetail {
  /** 持仓信息。 */
  holding: HoldingItem;
  /** 摘要。 */
  summary: HoldingDetailSummary;
  /** 交易记录。 */
  transactions: InvestmentTransactionItem[];
  /** 价格快照。 */
  priceSnapshots: AssetPriceItem[];
  /** 图表点位。 */
  chartPoints: HoldingChartPoint[];
  /** 收益日历。 */
  profitCalendar?: InvestmentCalendarDayProfit[];
}

/** 投资趋势点。 */
export interface InvestmentTrendPoint {
  /** 日期。 */
  date: string;
  /** 市值。 */
  marketValue: number;
  /** 总收益。 */
  totalProfit: number;
  /** 资产金额。 */
  assetAmount?: number | null;
  /** 持有收益。 */
  holdingProfit?: number | null;
  /** 当日收益。 */
  dailyProfit?: number | null;
  /** 当日收益率。 */
  dailyProfitRate?: number | null;
  /** 主收益名称。 */
  primaryProfitLabel?: string | null;
  /** 主收益金额。 */
  primaryProfitAmount?: number | null;
}

/** 投资模块资产统计。 */
export interface InvestmentModuleAsset {
  /** 模块。 */
  module: 'FUND' | 'STOCK' | 'CRYPTO';
  /** 名称。 */
  name: string;
  /** 资产金额。 */
  assetAmount: number;
  /** 资产占比。 */
  assetRatio: number;
  /** 主收益名称。 */
  primaryProfitLabel: string;
  /** 主收益是否可用。 */
  primaryProfitAvailable?: boolean | null;
  /** 主收益金额。 */
  primaryProfitAmount?: number | null;
  /** 主收益状态。 */
  primaryProfitStatusLabel?: string | null;
  /** 昨日收益。 */
  yesterdayProfit?: number | null;
  /** 昨日收益率。 */
  yesterdayProfitRate?: number | null;
  /** 持有收益。 */
  holdingProfit: number;
  /** 持有收益率。 */
  holdingProfitRate: number;
  /** 持仓数量。 */
  holdingCount: number;
}

/** 投资总览。 */
export interface InvestmentOverview {
  /** 投资资产总额。 */
  totalInvestmentAsset: number;
  /** 总成本。 */
  totalCost: number;
  /** 持有收益。 */
  holdingProfit: number;
  /** 持有收益率。 */
  holdingProfitRate: number;
  /** 今日收益。 */
  todayProfit?: number | null;
  /** 今日收益是否可用。 */
  todayProfitAvailable?: boolean | null;
  /** 今日收益范围。 */
  todayProfitAssetScope: string;
  /** 今日收益状态。 */
  todayProfitStatusLabel?: string | null;
  /** 昨日收益。 */
  yesterdayProfit?: number | null;
  /** 昨日收益范围。 */
  yesterdayProfitAssetScope: string;
  /** 模块资产列表。 */
  moduleAssets: InvestmentModuleAsset[];
}

/** 投资趋势。 */
export interface InvestmentTrend {
  /** 模块。 */
  module: string;
  /** 周期。 */
  period: string;
  /** 趋势点。 */
  points: InvestmentTrendPoint[];
}

/** 持仓保存参数。 */
export interface HoldingRequest {
  /** 资产ID。 */
  assetId?: string | null;
  /** 资产名称。 */
  assetName?: string;
  /** 代码。 */
  symbol?: string;
  /** 资产类型。 */
  assetType?: AssetType;
  /** 市场。 */
  market?: string;
  /** 币种。 */
  currency?: string;
  /** 行情来源。 */
  quoteSource?: QuoteSource;
  /** 行情键。 */
  quoteKey?: string;
  /** 最新价。 */
  latestPrice?: number;
  /** 昨收价。 */
  previousClose?: number | null;
  /** 涨跌幅。 */
  changePercent?: number | null;
  /** 报价时间。 */
  quoteTime?: string | null;
  /** 市场状态。 */
  marketStatus?: string;
  /** 数量。 */
  quantity: number;
  /** 平均成本。 */
  avgCost: number;
  /** 备注。 */
  remark?: string;
}

/** 投资交易项。 */
export interface InvestmentTransactionItem {
  /** ID。 */
  id: string;
  /** 持仓ID。 */
  holdingId: string;
  /** 资产ID。 */
  assetId: string;
  /** 账户ID。 */
  accountId: string;
  /** 账户名称。 */
  accountName: string | null;
  /** 资产名称。 */
  assetName: string | null;
  /** 代码。 */
  symbol: string | null;
  /** 类型。 */
  type: InvestmentTransactionType;
  /** 录入模式。 */
  inputMode?: InvestmentInputMode | null;
  /** 交易金额。 */
  tradeAmount?: number | null;
  /** 成交数量。 */
  tradeQuantity?: number | null;
  /** 成交价格。 */
  tradePrice?: number | null;
  /** 数量。 */
  quantity: number;
  /** 价格。 */
  price: number;
  /** 金额。 */
  amount: number;
  /** 手续费。 */
  fee: number;
  /** 成本金额。 */
  costAmount?: number | null;
  /** 已实现收益。 */
  realizedProfit?: number | null;
  /** 交易日期。 */
  tradeDate?: string | null;
  /** 确认日期。 */
  confirmedDate?: string | null;
  /** 确认净值。 */
  confirmedNav?: number | null;
  /** 确认份额。 */
  confirmedQuantity?: number | null;
  /** 状态。 */
  status?: InvestmentTransactionStatus | null;
  /** 撤销时间。 */
  revokeTime?: string | null;
  /** 撤销原因。 */
  revokeReason?: string | null;
  /** 交易时间。 */
  transactionTime: string;
  /** 备注。 */
  note?: string | null;
}

/** 投资交易保存参数。 */
export interface InvestmentTransactionRequest {
  /** 持仓ID。 */
  holdingId?: string | null;
  /** 资产ID。 */
  assetId: string;
  /** 账户ID。 */
  accountId: string;
  /** 类型。 */
  type: InvestmentTransactionType;
  /** 录入模式。 */
  inputMode?: InvestmentInputMode;
  /** 交易金额。 */
  tradeAmount?: number;
  /** 数量。 */
  quantity?: number;
  /** 价格。 */
  price?: number;
  /** 确认日期。 */
  confirmedDate?: string;
  /** 手续费。 */
  fee: number;
  /** 交易时间。 */
  transactionTime: string;
  /** 备注。 */
  note?: string;
}

/** 基金确认预估。 */
export interface FundConfirmPreview {
  /** 交易日期。 */
  tradeDate: string;
  /** 有效申请日。 */
  effectiveTradeDate: string;
  /** 确认日期。 */
  confirmedDate: string;
  /** 是否QDII。 */
  qdii: boolean;
  /** 是否顺延。 */
  shifted: boolean;
  /** 顺延原因。 */
  shiftReason?: string | null;
}

/** 手动行情参数。 */
export interface ManualQuoteRequest {
  /** 资产ID。 */
  assetId: string;
  /** 价格。 */
  price: number;
  /** 币种。 */
  currency: string;
  /** 报价时间。 */
  quoteTime?: string;
}

/** 单个行情刷新参数。 */
export interface RefreshQuoteRequest {
  /** 资产ID。 */
  assetId: string;
}

/** 批量行情刷新参数。 */
export interface BatchRefreshQuoteRequest {
  /** 资产ID列表。 */
  assetIds: string[];
}

export const investmentApi = {
  // 搜索公共资产。
  searchAssets(params: { keyword?: string; type?: AssetType }) {
    return request<AssetItem[]>({
      url: '/assets/search',
      method: 'GET',
      params
    });
  },
  // 识别资产行情信息。
  lookupAssets(params: { type: AssetType; keyword: string; market?: string }) {
    return request<AssetLookupItem[]>({
      url: '/assets/lookup',
      method: 'GET',
      params
    });
  },
  // 创建公共资产。
  createAsset(data: AssetRequest) {
    return request<AssetItem>({
      url: '/assets',
      method: 'POST',
      data
    });
  },
  // 查询持仓列表。
  listHoldings() {
    return request<HoldingItem[]>({
      url: '/holdings',
      method: 'GET'
    });
  },
  // 查询投资总览。
  overviewInvestments() {
    return request<InvestmentOverview>({
      url: '/investments/overview',
      method: 'GET'
    });
  },
  // 按模块查询持仓。
  listInvestmentHoldings(params?: { module?: 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO' }) {
    return request<HoldingItem[]>({
      url: '/investments/holdings',
      method: 'GET',
      params
    });
  },
  // 查询持仓汇总。
  summaryHoldings() {
    return request<HoldingSummary>({
      url: '/holdings/summary',
      method: 'GET'
    });
  },
  // 查询持仓详情。
  detailHolding(id: string) {
    return request<HoldingDetail>({
      url: `/holdings/${id}/detail`,
      method: 'GET'
    });
  },
  // 查询持仓趋势。
  trendHoldings(params: { startDate?: string; endDate?: string }) {
    return request<InvestmentTrendPoint[]>({
      url: '/holdings/trend',
      method: 'GET',
      params
    });
  },
  // 查询投资模块趋势。
  trendInvestments(params: { module?: 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO'; period?: 'WEEK' | 'MONTH' | 'QUARTER' | 'YEAR'; startDate?: string; endDate?: string }) {
    return request<InvestmentTrend>({
      url: '/investments/trend',
      method: 'GET',
      params
    });
  },
  // 查询收益日历。
  profitCalendar(id: string, params?: { year?: number; month?: number }) {
    return request<InvestmentCalendarDayProfit[]>({
      url: `/investments/holdings/${id}/profit-calendar`,
      method: 'GET',
      params
    });
  },
  // 查询全持仓每日收益。
  dailyProfitCalendar(params?: { year?: number; month?: number }) {
    return request<InvestmentCalendarDayProfit[]>({
      url: '/investments/daily-profit',
      method: 'GET',
      params
    });
  },
  // 生成投资日快照。
  generateInvestmentSnapshot(params?: { snapshotDate?: string }) {
    return request<void>({
      url: '/investments/snapshots/generate',
      method: 'POST',
      params
    });
  },
  // 创建持仓。
  createHolding(data: HoldingRequest) {
    return request<HoldingItem>({
      url: '/holdings',
      method: 'POST',
      data
    });
  },
  // 更新持仓。
  updateHolding(id: string, data: HoldingRequest) {
    return request<HoldingItem>({
      url: `/holdings/${id}`,
      method: 'PUT',
      data
    });
  },
  // 创建投资交易。
  createTransaction(data: InvestmentTransactionRequest) {
    return request<InvestmentTransactionItem>({
      url: '/investment-transactions',
      method: 'POST',
      data
    });
  },
  // 预估基金确认日。
  fundConfirmPreview(params: { assetId: string; transactionTime: string }) {
    return request<FundConfirmPreview>({
      url: '/investment-transactions/fund-confirm-preview',
      method: 'GET',
      params
    });
  },
  // 查询投资交易列表。
  listTransactions(holdingId?: string) {
    return request<InvestmentTransactionItem[]>({
      url: '/investment-transactions',
      method: 'GET',
      params: holdingId ? { holdingId } : undefined
    });
  },
  // 撤销投资交易。
  revokeTransaction(id: string, reason?: string) {
    return request<InvestmentTransactionItem>({
      url: `/investment-transactions/${id}/revoke`,
      method: 'PUT',
      data: { reason }
    });
  },
  // 保存手动行情。
  manualQuote(data: ManualQuoteRequest) {
    return request({
      url: '/quotes/manual',
      method: 'POST',
      data
    });
  },
  // 刷新单个资产行情。
  refreshQuote(data: RefreshQuoteRequest) {
    return request({
      url: '/quotes/refresh',
      method: 'POST',
      data
    });
  },
  // 批量刷新资产行情。
  refreshQuotes(data: BatchRefreshQuoteRequest) {
    return request<AssetPriceItem[]>({
      url: '/quotes/refresh-batch',
      method: 'POST',
      data
    });
  }
};
