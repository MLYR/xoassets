// 汇率 API：前端只读取 XOAssets 后端缓存汇率，不直接访问第三方汇率源。
import { request } from './http';

export interface ExchangeRateItem {
  baseCurrency: string;
  targetCurrency: string;
  rate: number;
  source: string;
  quoteTime: string;
}

export const exchangeRateApi = {
  usdCny() {
    return request<ExchangeRateItem>({
      url: '/exchange-rates/usd-cny',
      method: 'GET'
    });
  }
};
