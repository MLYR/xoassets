-- 旧价格快照表退役迁移：把 xo_asset_price 历史价沉淀到 current/daily 后删除旧表。
-- 历史库存在 xo_asset_price 时会迁移数据；全新库或已迁移库执行时会创建空兼容表后直接删除，避免脚本失败。
-- 执行前建议先备份 xo_asset_price；如果需要人工核对，可先执行到 DROP TABLE 前。
USE xoassets;

CREATE TABLE IF NOT EXISTS xo_asset_price (
  id BIGINT PRIMARY KEY COMMENT '旧价格快照ID',
  asset_id BIGINT NOT NULL COMMENT '资产ID',
  price DECIMAL(28,8) NOT NULL COMMENT '价格',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  previous_close DECIMAL(28,8) DEFAULT NULL COMMENT '昨收价或上一交易日价格',
  change_amount DECIMAL(28,8) DEFAULT NULL COMMENT '涨跌额',
  change_percent DECIMAL(18,4) DEFAULT NULL COMMENT '涨跌幅百分比',
  source VARCHAR(30) NOT NULL COMMENT '来源',
  quote_time DATETIME NOT NULL COMMENT '报价时间',
  market_status VARCHAR(30) DEFAULT NULL COMMENT '市场状态',
  raw_json TEXT DEFAULT NULL COMMENT '行情原文',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_asset_time (asset_id, quote_time),
  KEY idx_quote_time (quote_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旧资产价格快照兼容表';

DELIMITER //
CREATE PROCEDURE xo_add_column_if_missing(IN p_table_name VARCHAR(64), IN p_column_name VARCHAR(64), IN p_column_ddl TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    SET @xo_add_column_sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_ddl);
    PREPARE xo_add_column_stmt FROM @xo_add_column_sql;
    EXECUTE xo_add_column_stmt;
    DEALLOCATE PREPARE xo_add_column_stmt;
  END IF;
END//
DELIMITER ;

CALL xo_add_column_if_missing('xo_asset_price', 'previous_close', 'previous_close DECIMAL(28,8) DEFAULT NULL COMMENT ''昨收价或上一交易日价格'' AFTER currency');
CALL xo_add_column_if_missing('xo_asset_price', 'change_amount', 'change_amount DECIMAL(28,8) DEFAULT NULL COMMENT ''涨跌额'' AFTER previous_close');
CALL xo_add_column_if_missing('xo_asset_price', 'change_percent', 'change_percent DECIMAL(18,4) DEFAULT NULL COMMENT ''涨跌幅百分比'' AFTER change_amount');
CALL xo_add_column_if_missing('xo_asset_price', 'market_status', 'market_status VARCHAR(30) DEFAULT NULL COMMENT ''市场状态'' AFTER quote_time');
CALL xo_add_column_if_missing('xo_asset_price', 'raw_json', 'raw_json TEXT DEFAULT NULL COMMENT ''行情原文'' AFTER market_status');
CALL xo_add_column_if_missing('xo_asset_price', 'created_at', 'created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL xo_add_column_if_missing('xo_asset_price', 'updated_at', 'updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL xo_add_column_if_missing('xo_asset_price', 'deleted', 'deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0否 1是''');
DROP PROCEDURE xo_add_column_if_missing;

CREATE TABLE IF NOT EXISTS xo_asset_price_current (
  asset_id BIGINT PRIMARY KEY COMMENT '资产ID',
  price DECIMAL(28,8) NOT NULL COMMENT '最新价格',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  previous_close DECIMAL(28,8) DEFAULT NULL COMMENT '昨收价或上一交易日价格',
  change_amount DECIMAL(28,8) DEFAULT NULL COMMENT '涨跌额',
  change_percent DECIMAL(18,4) DEFAULT NULL COMMENT '涨跌幅百分比',
  source VARCHAR(30) NOT NULL COMMENT '行情来源',
  quote_time DATETIME NOT NULL COMMENT '报价时间',
  market_status VARCHAR(30) DEFAULT NULL COMMENT '市场状态',
  raw_json TEXT DEFAULT NULL COMMENT '行情原文',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_quote_time (quote_time),
  KEY idx_source_time (source, quote_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产当前价格表';

CREATE TABLE IF NOT EXISTS xo_asset_price_daily (
  id BIGINT PRIMARY KEY COMMENT 'ID',
  asset_id BIGINT NOT NULL COMMENT '资产ID',
  trade_date DATE NOT NULL COMMENT '交易日期',
  open_price DECIMAL(28,8) DEFAULT NULL COMMENT '开盘/首条价格',
  close_price DECIMAL(28,8) NOT NULL COMMENT '收盘/最后价格',
  high_price DECIMAL(28,8) DEFAULT NULL COMMENT '最高价',
  low_price DECIMAL(28,8) DEFAULT NULL COMMENT '最低价',
  previous_close DECIMAL(28,8) DEFAULT NULL COMMENT '上一交易日收盘价',
  change_amount DECIMAL(28,8) DEFAULT NULL COMMENT '较上一交易日涨跌额',
  change_percent DECIMAL(18,4) DEFAULT NULL COMMENT '较上一交易日涨跌幅',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  source VARCHAR(30) NOT NULL COMMENT '行情来源',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  UNIQUE KEY uk_asset_trade_date (asset_id, trade_date, deleted),
  KEY idx_asset_date (asset_id, trade_date),
  KEY idx_trade_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产日级价格表';

-- 1) 每个资产最新一条旧快照迁入 current；如果 current 已存在，仅当旧快照报价时间更新时覆盖。
INSERT INTO xo_asset_price_current (
  asset_id, price, currency, previous_close, change_amount, change_percent,
  source, quote_time, market_status, raw_json, created_at, updated_at
)
SELECT asset_id, price, currency, previous_close, change_amount, change_percent,
       source, quote_time, market_status, raw_json, created_at, updated_at
FROM (
  SELECT p.*,
         ROW_NUMBER() OVER (PARTITION BY p.asset_id ORDER BY p.quote_time DESC, p.created_at DESC, p.id DESC) AS rn
  FROM xo_asset_price p
  WHERE p.deleted = 0 AND p.price > 0 AND p.quote_time IS NOT NULL
) latest
WHERE latest.rn = 1
ON DUPLICATE KEY UPDATE
  price = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(price), xo_asset_price_current.price),
  currency = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(currency), xo_asset_price_current.currency),
  previous_close = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(previous_close), xo_asset_price_current.previous_close),
  change_amount = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(change_amount), xo_asset_price_current.change_amount),
  change_percent = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(change_percent), xo_asset_price_current.change_percent),
  source = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(source), xo_asset_price_current.source),
  quote_time = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(quote_time), xo_asset_price_current.quote_time),
  market_status = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(market_status), xo_asset_price_current.market_status),
  raw_json = IF(VALUES(quote_time) >= xo_asset_price_current.quote_time, VALUES(raw_json), xo_asset_price_current.raw_json),
  updated_at = CURRENT_TIMESTAMP;

-- 2) 按 asset_id + 日期聚合旧快照到 daily，保留 open/close/high/low 和上一交易日收盘价。
SET @daily_id := UNIX_TIMESTAMP(NOW(3)) * 1000;

INSERT INTO xo_asset_price_daily (
  id, asset_id, trade_date, open_price, close_price, high_price, low_price,
  previous_close, change_amount, change_percent, currency, source, created_at, updated_at, deleted
)
WITH ranked AS (
  SELECT p.*,
         DATE(p.quote_time) AS trade_date,
         ROW_NUMBER() OVER (PARTITION BY p.asset_id, DATE(p.quote_time) ORDER BY p.quote_time ASC, p.created_at ASC, p.id ASC) AS open_rn,
         ROW_NUMBER() OVER (PARTITION BY p.asset_id, DATE(p.quote_time) ORDER BY p.quote_time DESC, p.created_at DESC, p.id DESC) AS close_rn
  FROM xo_asset_price p
  WHERE p.deleted = 0 AND p.price > 0 AND p.quote_time IS NOT NULL
), daily AS (
  SELECT r.asset_id,
         r.trade_date,
         MAX(CASE WHEN r.open_rn = 1 THEN r.price END) AS open_price,
         MAX(CASE WHEN r.close_rn = 1 THEN r.price END) AS close_price,
         MAX(r.price) AS high_price,
         MIN(r.price) AS low_price,
         MAX(CASE WHEN r.close_rn = 1 THEN r.currency END) AS currency,
         MAX(CASE WHEN r.close_rn = 1 THEN r.source END) AS source,
         MIN(r.created_at) AS created_at,
         MAX(r.updated_at) AS updated_at
  FROM ranked r
  GROUP BY r.asset_id, r.trade_date
), with_prev AS (
  SELECT d.*,
         LAG(d.close_price) OVER (PARTITION BY d.asset_id ORDER BY d.trade_date) AS previous_close
  FROM daily d
)
SELECT (@daily_id := @daily_id + 1) AS id,
       asset_id,
       trade_date,
       open_price,
       close_price,
       high_price,
       low_price,
       previous_close,
       CASE WHEN previous_close IS NULL THEN NULL ELSE close_price - previous_close END AS change_amount,
       CASE WHEN previous_close IS NULL OR previous_close <= 0 THEN NULL ELSE ROUND((close_price - previous_close) * 100 / previous_close, 4) END AS change_percent,
       currency,
       source,
       created_at,
       updated_at,
       0 AS deleted
FROM with_prev
ON DUPLICATE KEY UPDATE
  open_price = VALUES(open_price),
  close_price = VALUES(close_price),
  high_price = VALUES(high_price),
  low_price = VALUES(low_price),
  previous_close = VALUES(previous_close),
  change_amount = VALUES(change_amount),
  change_percent = VALUES(change_percent),
  currency = VALUES(currency),
  source = VALUES(source),
  updated_at = CURRENT_TIMESTAMP;

-- 3) 应用代码已不再访问旧表；确认 current/daily 数据无误后删除。
DROP TABLE IF EXISTS xo_asset_price;
