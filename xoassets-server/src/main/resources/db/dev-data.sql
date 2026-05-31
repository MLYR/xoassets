-- XOAssets 小〇财迹开发验收数据。
-- 使用方式：先执行 schema.sql，再执行本文件；测试账号 demo / xoassets123。
USE xoassets;

-- 开发数据使用固定 ID，便于前后端联调和验收脚本重复定位。
DELETE FROM xo_ai_report;
DELETE FROM xo_goal;
DELETE FROM xo_budget;
DELETE FROM xo_asset_price;
DELETE FROM xo_investment_transaction;
DELETE FROM xo_holding;
DELETE FROM xo_asset;
DELETE FROM xo_transaction;
DELETE FROM xo_category;
DELETE FROM xo_account;
DELETE FROM xo_user;

INSERT INTO xo_user (id, username, password_hash, nickname, status, created_at, updated_at, deleted)
VALUES
  (1000000000000000001, 'demo', '$2a$10$alZvnjh3VxQfMuLAJBy4R.Z6uHXNErWom.R/NbBDzdYoRQ/F3I.Lq', '演示用户', 1, NOW(), NOW(), 0),
  (1000000000000000002, 'other', '$2a$10$alZvnjh3VxQfMuLAJBy4R.Z6uHXNErWom.R/NbBDzdYoRQ/F3I.Lq', '隔离用户', 1, NOW(), NOW(), 0);

INSERT INTO xo_account (id, user_id, name, type, balance, initial_balance, currency, status, sort_order, remark, created_at, updated_at, deleted)
VALUES
  (1100000000000000001, 1000000000000000001, '招商银行卡', 'BANK', 21500.0000, 10000.0000, 'CNY', 1, 1, '工资和大额支出账户', NOW(), NOW(), 0),
  (1100000000000000002, 1000000000000000001, '支付宝', 'ALIPAY', 1933.5000, 1500.0000, 'CNY', 1, 2, '日常消费账户', NOW(), NOW(), 0),
  (1100000000000000101, 1000000000000000002, '隔离账户', 'BANK', 999.0000, 999.0000, 'CNY', 1, 1, '用于验证 user_id 隔离', NOW(), NOW(), 0);

INSERT INTO xo_category (id, user_id, name, type, icon, color, status, sort_order, created_at, updated_at, deleted)
VALUES
  (1200000000000000001, 1000000000000000001, '工资', 'INCOME', 'Wallet', '#16a34a', 1, 1, NOW(), NOW(), 0),
  (1200000000000000002, 1000000000000000001, '退款', 'INCOME', 'RefreshCcw', '#0ea5e9', 1, 2, NOW(), NOW(), 0),
  (1200000000000000101, 1000000000000000001, '餐饮', 'EXPENSE', 'Utensils', '#ef4444', 1, 1, NOW(), NOW(), 0),
  (1200000000000000102, 1000000000000000001, '交通', 'EXPENSE', 'Bus', '#f59e0b', 1, 2, NOW(), NOW(), 0),
  (1200000000000000103, 1000000000000000001, '购物', 'EXPENSE', 'ShoppingBag', '#8b5cf6', 1, 3, NOW(), NOW(), 0),
  (1200000000000000201, 1000000000000000002, '隔离支出', 'EXPENSE', 'Lock', '#64748b', 1, 1, NOW(), NOW(), 0);

INSERT INTO xo_transaction (id, user_id, type, amount, account_id, target_account_id, category_id, original_transaction_id, transaction_time, note, image_url, status, created_at, updated_at, deleted)
VALUES
  (1300000000000000001, 1000000000000000001, 'INCOME', 12000.0000, 1100000000000000001, NULL, 1200000000000000001, NULL, '2026-05-05 09:00:00', '5 月工资', NULL, 1, NOW(), NOW(), 0),
  (1300000000000000002, 1000000000000000001, 'EXPENSE', 86.5000, 1100000000000000002, NULL, 1200000000000000101, NULL, '2026-05-08 12:30:00', '午餐和咖啡', NULL, 1, NOW(), NOW(), 0),
  (1300000000000000003, 1000000000000000001, 'TRANSFER', 500.0000, 1100000000000000001, 1100000000000000002, NULL, NULL, '2026-05-10 18:00:00', '转入支付宝备用', NULL, 1, NOW(), NOW(), 0),
  (1300000000000000004, 1000000000000000001, 'REFUND', 20.0000, 1100000000000000002, NULL, 1200000000000000101, 1300000000000000002, '2026-05-12 10:00:00', '餐饮优惠退款', NULL, 1, NOW(), NOW(), 0),
  (1300000000000000101, 1000000000000000002, 'EXPENSE', 10.0000, 1100000000000000101, NULL, 1200000000000000201, NULL, '2026-05-08 12:30:00', '隔离用户流水', NULL, 1, NOW(), NOW(), 0);

INSERT INTO xo_asset (id, symbol, name, type, currency, quote_source, quote_key, status, created_at, updated_at, deleted)
VALUES
  (1400000000000000001, 'DOGE', 'Dogecoin', 'CRYPTO', 'USD', 'COINGECKO', 'DOGE', 1, NOW(), NOW(), 0),
  (1400000000000000002, '510300', '沪深300ETF', 'FUND', 'CNY', 'MANUAL', '510300', 1, NOW(), NOW(), 0);

INSERT INTO xo_holding (id, user_id, asset_id, quantity, avg_cost, total_cost, remark, status, created_at, updated_at, deleted)
VALUES
  (1500000000000000001, 1000000000000000001, 1400000000000000001, 881.3220, 0.6000, 528.7932, 'DOGE 验收持仓，市值应为 638.3592 USD', 1, NOW(), NOW(), 0),
  (1500000000000000002, 1000000000000000001, 1400000000000000002, 1000.0000, 1.2345, 1234.5000, '基金 A 验收持仓', 1, NOW(), NOW(), 0);

INSERT INTO xo_investment_transaction (id, user_id, holding_id, asset_id, type, quantity, price, amount, fee, transaction_time, note, created_at, updated_at, deleted)
VALUES
  (1600000000000000001, 1000000000000000001, 1500000000000000001, 1400000000000000001, 'BUY', 1000.0000, 0.6000, 600.0000, 0.0000, '2026-05-03 10:00:00', '买入 DOGE', NOW(), NOW(), 0),
  (1600000000000000002, 1000000000000000001, 1500000000000000001, 1400000000000000001, 'SELL', 118.6780, 0.7000, 83.0746, 0.0000, '2026-05-20 10:00:00', '卖出部分 DOGE', NOW(), NOW(), 0),
  (1600000000000000003, 1000000000000000001, 1500000000000000002, 1400000000000000002, 'BUY', 1000.0000, 1.2345, 1234.5000, 0.0000, '2026-05-06 14:00:00', '买入基金 A', NOW(), NOW(), 0);

INSERT INTO xo_asset_price (id, asset_id, price, currency, source, quote_time, raw_json, created_at, updated_at, deleted)
VALUES
  (1700000000000000001, 1400000000000000001, 0.72432000, 'USD', 'MANUAL', '2026-05-31 09:00:00', '{"demo":true}', NOW(), NOW(), 0),
  (1700000000000000002, 1400000000000000002, 1.25000000, 'CNY', 'MANUAL', '2026-05-31 09:00:00', '{"demo":true}', NOW(), NOW(), 0);

INSERT INTO xo_budget (id, user_id, month, category_id, budget_type, amount, status, created_at, updated_at, deleted)
VALUES
  (1800000000000000001, 1000000000000000001, '2026-05', NULL, 'TOTAL', 5000.0000, 1, NOW(), NOW(), 0),
  (1800000000000000002, 1000000000000000001, '2026-05', 1200000000000000101, 'CATEGORY', 1200.0000, 1, NOW(), NOW(), 0);

INSERT INTO xo_goal (id, user_id, name, target_amount, current_amount, target_date, status, created_at, updated_at, deleted)
VALUES
  (1900000000000000001, 1000000000000000001, '年度净资产目标', 100000.0000, 24071.8592, '2026-12-31', 'ACTIVE', NOW(), NOW(), 0);

INSERT INTO xo_ai_report (id, user_id, report_type, report_date, title, content, summary_json, status, created_at, updated_at, deleted)
VALUES
  (2000000000000000001, 1000000000000000001, 'DAILY', '2026-05-31', '2026-05-31 财务复盘', '今日总资产保持稳定，餐饮预算使用较低，DOGE 当前价按 0.724320 USD 估值。报告为模板数据，不包含投资建议。', '{"income":12000.00,"expense":66.50,"investmentProfit":125.0660}', 'SUCCESS', NOW(), NOW(), 0);
