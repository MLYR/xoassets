-- 持仓每日收益持久化：收益日历、昨日收益、趋势每日收益统一从该表聚合。
USE xoassets;

-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，这里按 information_schema 动态补列，保证迁移可重复执行。
SET @calendar_profit_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'calendar_profit'
);
SET @calendar_profit_sql := IF(@calendar_profit_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN calendar_profit DECIMAL(18,4) DEFAULT NULL COMMENT ''收益日历展示日收益，按持仓每日收益聚合'' AFTER net_inflow',
  'SELECT 1'
);
PREPARE calendar_profit_stmt FROM @calendar_profit_sql;
EXECUTE calendar_profit_stmt;
DEALLOCATE PREPARE calendar_profit_stmt;

SET @calendar_profit_rate_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'calendar_profit_rate'
);
SET @calendar_profit_rate_sql := IF(@calendar_profit_rate_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN calendar_profit_rate DECIMAL(18,4) DEFAULT NULL COMMENT ''收益日历展示日收益率'' AFTER calendar_profit',
  'SELECT 1'
);
PREPARE calendar_profit_rate_stmt FROM @calendar_profit_rate_sql;
EXECUTE calendar_profit_rate_stmt;
DEALLOCATE PREPARE calendar_profit_rate_stmt;

SET @calendar_base_amount_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'calendar_base_amount'
);
SET @calendar_base_amount_sql := IF(@calendar_base_amount_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN calendar_base_amount DECIMAL(18,4) DEFAULT NULL COMMENT ''收益日历收益率基准金额'' AFTER calendar_profit_rate',
  'SELECT 1'
);
PREPARE calendar_base_amount_stmt FROM @calendar_base_amount_sql;
EXECUTE calendar_base_amount_stmt;
DEALLOCATE PREPARE calendar_base_amount_stmt;

SET @buy_amount_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'buy_amount'
);
SET @buy_amount_sql := IF(@buy_amount_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN buy_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT ''当日买入本金合计'' AFTER calendar_base_amount',
  'SELECT 1'
);
PREPARE buy_amount_stmt FROM @buy_amount_sql;
EXECUTE buy_amount_stmt;
DEALLOCATE PREPARE buy_amount_stmt;

SET @sell_amount_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'sell_amount'
);
SET @sell_amount_sql := IF(@sell_amount_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN sell_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT ''当日卖出成交本金合计'' AFTER buy_amount',
  'SELECT 1'
);
PREPARE sell_amount_stmt FROM @sell_amount_sql;
EXECUTE sell_amount_stmt;
DEALLOCATE PREPARE sell_amount_stmt;

SET @fee_amount_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'fee_amount'
);
SET @fee_amount_sql := IF(@fee_amount_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN fee_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT ''当日投资交易手续费合计'' AFTER sell_amount',
  'SELECT 1'
);
PREPARE fee_amount_stmt FROM @fee_amount_sql;
EXECUTE fee_amount_stmt;
DEALLOCATE PREPARE fee_amount_stmt;

SET @buy_count_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'buy_count'
);
SET @buy_count_sql := IF(@buy_count_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN buy_count INT NOT NULL DEFAULT 0 COMMENT ''当日买入笔数'' AFTER fee_amount',
  'SELECT 1'
);
PREPARE buy_count_stmt FROM @buy_count_sql;
EXECUTE buy_count_stmt;
DEALLOCATE PREPARE buy_count_stmt;

SET @sell_count_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_daily_snapshot'
    AND column_name = 'sell_count'
);
SET @sell_count_sql := IF(@sell_count_exists = 0,
  'ALTER TABLE xo_investment_daily_snapshot ADD COLUMN sell_count INT NOT NULL DEFAULT 0 COMMENT ''当日卖出笔数'' AFTER buy_count',
  'SELECT 1'
);
PREPARE sell_count_stmt FROM @sell_count_sql;
EXECUTE sell_count_stmt;
DEALLOCATE PREPARE sell_count_stmt;

CREATE TABLE IF NOT EXISTS xo_investment_holding_daily_profit (
  id BIGINT PRIMARY KEY COMMENT 'ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  holding_id BIGINT NOT NULL COMMENT '持仓ID',
  asset_id BIGINT NOT NULL COMMENT '资产ID',
  asset_type VARCHAR(20) NOT NULL COMMENT '资产类型',
  module VARCHAR(20) NOT NULL COMMENT '资产模块：FUND STOCK CRYPTO OTHER',
  display_date DATE NOT NULL COMMENT '收益日历展示日期',
  price_date DATE NOT NULL COMMENT '实际价格或净值日期',
  previous_price_date DATE DEFAULT NULL COMMENT '上一个价格日期',
  quantity_date DATE DEFAULT NULL COMMENT '收益基准份额日期',
  quantity DECIMAL(28,10) NOT NULL DEFAULT 0 COMMENT '收益基准数量',
  price DECIMAL(28,8) DEFAULT NULL COMMENT '展示日价格',
  previous_price DECIMAL(28,8) DEFAULT NULL COMMENT '上一价格',
  profit_amount DECIMAL(18,4) DEFAULT NULL COMMENT '收益金额',
  profit_rate DECIMAL(18,4) DEFAULT NULL COMMENT '收益率',
  base_amount DECIMAL(18,4) DEFAULT NULL COMMENT '收益率基准金额',
  market_value DECIMAL(18,4) DEFAULT NULL COMMENT '持仓市值',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  status VARCHAR(30) NOT NULL DEFAULT 'NORMAL' COMMENT '计算状态：NORMAL MARKET_CLOSED PRICE_MISSING',
  status_label VARCHAR(50) DEFAULT NULL COMMENT '状态文案',
  calc_version INT NOT NULL DEFAULT 1 COMMENT '计算版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  UNIQUE KEY uk_holding_price_date (holding_id, price_date, deleted),
  KEY idx_user_display_date (user_id, display_date),
  KEY idx_user_holding_display_date (user_id, holding_id, display_date),
  KEY idx_asset_price_date (asset_id, price_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持仓每日收益表';
