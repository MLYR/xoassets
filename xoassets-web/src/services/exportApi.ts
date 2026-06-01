// 导出 API：统一处理 CSV Blob 下载，筛选参数由页面传入。
import { request } from './http';
import type { AccountLedgerQuery } from './accountApi';
import type { TransactionQuery } from './transactionApi';

export interface InvestmentTransactionExportQuery {
  startDate?: string;
  endDate?: string;
  holdingId?: string;
  assetId?: string;
  accountId?: string;
}

export const exportApi = {
  // 导出账户资金明细。
  accountLedger(params: AccountLedgerQuery & { accountId: string }) {
    return download('/export/account-ledger', params, `account-ledger-${today()}.csv`);
  },
  // 导出普通流水。
  transactions(params: TransactionQuery) {
    return download('/export/transactions', params, `transactions-${today()}.csv`);
  },
  // 导出投资交易。
  investmentTransactions(params: InvestmentTransactionExportQuery = {}) {
    return download('/export/investment-transactions', params, `investment-transactions-${today()}.csv`);
  }
};

async function download(url: string, params: object, filename: string) {
  const blob = await request<Blob>({
    url,
    method: 'GET',
    params,
    responseType: 'blob'
  });
  // 浏览器端直接创建临时链接下载，后端已加 UTF-8 BOM 兼容 Excel。
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(objectUrl);
}

function today() {
  return new Date().toISOString().slice(0, 10);
}
