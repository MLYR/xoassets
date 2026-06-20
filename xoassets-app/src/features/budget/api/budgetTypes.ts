export type BudgetType = 'TOTAL' | 'CATEGORY';

export interface BudgetItem {
  id: string;
  month?: string | null;
  categoryId?: string | null;
  categoryName?: string | null;
  budgetType?: BudgetType | string | null;
  amount?: number | null;
  usedAmount?: number | null;
  remainingAmount?: number | null;
  usageRate?: number | null;
  usageStatus?: string | null;
  usageStatusLabel?: string | null;
  status?: number | null;
}

export interface BudgetSummary {
  month?: string | null;
  totalBudget?: number | null;
  totalUsed?: number | null;
  totalRemaining?: number | null;
  usageRate?: number | null;
  usageStatus?: string | null;
  usageStatusLabel?: string | null;
  items?: BudgetItem[] | null;
}

export interface BudgetRequest {
  month: string;
  categoryId?: string | null;
  budgetType: BudgetType;
  amount: string;
  status?: number;
}

export interface ExpenseCategoryItem {
  id: string;
  name?: string | null;
  type?: string | null;
  status?: number | null;
}
