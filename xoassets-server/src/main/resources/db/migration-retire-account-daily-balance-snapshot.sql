-- 账户日余额快照表退役迁移：余额趋势已改为从普通流水、投资交易和余额修正事件实时重建。
USE xoassets;

DROP TABLE IF EXISTS xo_account_daily_balance_snapshot;
