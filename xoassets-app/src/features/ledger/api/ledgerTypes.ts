export type LedgerTransactionType = 'EXPENSE' | 'INCOME' | 'TRANSFER';

export interface LedgerAccount {
  id: string;
  name?: string | null;
  type?: string | null;
  balance?: number | null;
  currency?: string | null;
  status?: number | null;
}

export interface LedgerCategory {
  id: string;
  name?: string | null;
  type?: 'EXPENSE' | 'INCOME' | string | null;
  icon?: string | null;
  color?: string | null;
  status?: number | null;
}

export interface LedgerTransaction {
  id: string;
  type?: LedgerTransactionType | 'REFUND' | string | null;
  amount?: number | null;
  accountId?: string | null;
  accountName?: string | null;
  targetAccountId?: string | null;
  targetAccountName?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  transactionTime?: string | null;
  note?: string | null;
  imageUrl?: string | null;
  status?: number | null;
}

export interface LedgerTransactionRequest {
  type: LedgerTransactionType;
  amount: string;
  accountId: string;
  targetAccountId?: string | null;
  categoryId?: string | null;
  transactionTime: string;
  note?: string | null;
}
