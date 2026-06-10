-- 投资日快照字段注释修正：明确 daily_profit 是快照日资金流调整收益，不等同于收益日历展示值。
USE xoassets;

ALTER TABLE xo_investment_daily_snapshot
  MODIFY COLUMN daily_profit DECIMAL(18,4) DEFAULT NULL COMMENT '快照日资金流调整收益：本日投资市值 - 上一快照日投资市值 - 当日投资本金净流入',
  MODIFY COLUMN daily_profit_rate DECIMAL(18,4) DEFAULT NULL COMMENT '快照日资金流调整收益率',
  MODIFY COLUMN net_inflow DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '当日投资本金净流入：买入为正，卖出为负';
