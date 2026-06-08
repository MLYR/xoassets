-- 基金金额买入改造：支持待确认交易和确认净值 / 确认份额。
USE xoassets;

-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，这里按 information_schema 动态补列，保证迁移可重复执行。
SET @input_mode_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'input_mode'
);
SET @input_mode_sql := IF(@input_mode_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN input_mode VARCHAR(30) NOT NULL DEFAULT ''QUANTITY_PRICE'' COMMENT ''录入模式：QUANTITY_PRICE数量价格 AMOUNT_NAV金额净值'' AFTER type',
  'SELECT 1'
);
PREPARE input_mode_stmt FROM @input_mode_sql;
EXECUTE input_mode_stmt;
DEALLOCATE PREPARE input_mode_stmt;

SET @trade_amount_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'trade_amount'
);
SET @trade_amount_sql := IF(@trade_amount_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN trade_amount DECIMAL(18,4) DEFAULT NULL COMMENT ''用户录入交易总金额'' AFTER input_mode',
  'SELECT 1'
);
PREPARE trade_amount_stmt FROM @trade_amount_sql;
EXECUTE trade_amount_stmt;
DEALLOCATE PREPARE trade_amount_stmt;

SET @trade_quantity_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'trade_quantity'
);
SET @trade_quantity_sql := IF(@trade_quantity_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN trade_quantity DECIMAL(28,10) DEFAULT NULL COMMENT ''用户录入交易数量'' AFTER trade_amount',
  'SELECT 1'
);
PREPARE trade_quantity_stmt FROM @trade_quantity_sql;
EXECUTE trade_quantity_stmt;
DEALLOCATE PREPARE trade_quantity_stmt;

SET @trade_price_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'trade_price'
);
SET @trade_price_sql := IF(@trade_price_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN trade_price DECIMAL(18,6) DEFAULT NULL COMMENT ''用户录入成交价或净值'' AFTER trade_quantity',
  'SELECT 1'
);
PREPARE trade_price_stmt FROM @trade_price_sql;
EXECUTE trade_price_stmt;
DEALLOCATE PREPARE trade_price_stmt;

SET @trade_date_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'trade_date'
);
SET @trade_date_sql := IF(@trade_date_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN trade_date DATE DEFAULT NULL COMMENT ''交易发生日期'' AFTER realized_profit',
  'SELECT 1'
);
PREPARE trade_date_stmt FROM @trade_date_sql;
EXECUTE trade_date_stmt;
DEALLOCATE PREPARE trade_date_stmt;

SET @confirmed_date_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'confirmed_date'
);
SET @confirmed_date_sql := IF(@confirmed_date_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN confirmed_date DATE DEFAULT NULL COMMENT ''基金确认日期'' AFTER trade_date',
  'SELECT 1'
);
PREPARE confirmed_date_stmt FROM @confirmed_date_sql;
EXECUTE confirmed_date_stmt;
DEALLOCATE PREPARE confirmed_date_stmt;

SET @confirmed_nav_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'confirmed_nav'
);
SET @confirmed_nav_sql := IF(@confirmed_nav_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN confirmed_nav DECIMAL(18,6) DEFAULT NULL COMMENT ''基金确认单位净值'' AFTER confirmed_date',
  'SELECT 1'
);
PREPARE confirmed_nav_stmt FROM @confirmed_nav_sql;
EXECUTE confirmed_nav_stmt;
DEALLOCATE PREPARE confirmed_nav_stmt;

SET @confirmed_quantity_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_investment_transaction'
    AND column_name = 'confirmed_quantity'
);
SET @confirmed_quantity_sql := IF(@confirmed_quantity_exists = 0,
  'ALTER TABLE xo_investment_transaction ADD COLUMN confirmed_quantity DECIMAL(28,10) DEFAULT NULL COMMENT ''基金确认份额'' AFTER confirmed_nav',
  'SELECT 1'
);
PREPARE confirmed_quantity_stmt FROM @confirmed_quantity_sql;
EXECUTE confirmed_quantity_stmt;
DEALLOCATE PREPARE confirmed_quantity_stmt;

UPDATE xo_investment_transaction
SET input_mode = COALESCE(input_mode, 'QUANTITY_PRICE'),
    trade_amount = COALESCE(trade_amount, amount + fee),
    trade_quantity = COALESCE(trade_quantity, quantity),
    trade_price = COALESCE(trade_price, price),
    trade_date = COALESCE(trade_date, DATE(transaction_time)),
    confirmed_date = COALESCE(confirmed_date, DATE(transaction_time)),
    confirmed_nav = COALESCE(confirmed_nav, price),
    confirmed_quantity = COALESCE(confirmed_quantity, quantity)
WHERE deleted = 0;
