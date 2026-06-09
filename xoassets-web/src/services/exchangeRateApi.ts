// 汇率 API：前端只读取 XOAssets 后端缓存汇率，不直接访问第三方汇率源。
import { request } from './http';

/** 汇率返回数据。 */
export interface ExchangeRateItem {
  /** 基准币种。 */
  baseCurrency: string;
  /** 目标币种。 */
  targetCurrency: string;
  /** 汇率。 */
  rate: number;
  /** 来源。 */
  source: string;
  /** 报价时间。 */
  quoteTime: string;
}

export const exchangeRateApi = {
  // 查询美元兑人民币汇率。
  usdCny() {
    return request<ExchangeRateItem>({
      url: '/exchange-rates/usd-cny',
      method: 'GET'
    });
  }
};
