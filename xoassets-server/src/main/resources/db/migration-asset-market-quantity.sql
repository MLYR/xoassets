-- 资产 market 与投资数量精度迁移脚本；已有本地库升级时在 schema.sql 后按需执行。
USE xoassets;

-- MySQL 不支持 ADD COLUMN IF NOT EXISTS；按元数据动态补列，保证新库和旧库都可重复执行。
SET @asset_market_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_asset'
    AND column_name = 'market'
);
SET @asset_market_sql := IF(@asset_market_exists = 0,
  'ALTER TABLE xo_asset ADD COLUMN market VARCHAR(30) NOT NULL DEFAULT ''UNKNOWN'' COMMENT ''交易市场：SH SZ BJ US CN_FUND CRYPTO UNKNOWN'' AFTER type',
  'SELECT 1'
);
PREPARE asset_market_stmt FROM @asset_market_sql;
EXECUTE asset_market_stmt;
DEALLOCATE PREPARE asset_market_stmt;

UPDATE xo_asset
SET market = CASE
  WHEN type = 'CRYPTO' THEN 'CRYPTO'
  WHEN type = 'FUND' THEN 'CN_FUND'
  WHEN type = 'STOCK' AND symbol LIKE '%.SH' THEN 'SH'
  WHEN type = 'STOCK' AND symbol LIKE '%.SZ' THEN 'SZ'
  WHEN type = 'STOCK' AND symbol LIKE '%.BJ' THEN 'BJ'
  WHEN type = 'STOCK' THEN 'US'
  ELSE 'UNKNOWN'
END
WHERE market = 'UNKNOWN';

-- 老唯一索引和新索引可能因 schema.sql 已更新而不存在 / 已存在，逐项判断避免重复执行失败。
SET @old_asset_unique_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_asset'
    AND index_name = 'uk_type_symbol_deleted'
);
SET @old_asset_unique_sql := IF(@old_asset_unique_exists > 0,
  'ALTER TABLE xo_asset DROP INDEX uk_type_symbol_deleted',
  'SELECT 1'
);
PREPARE old_asset_unique_stmt FROM @old_asset_unique_sql;
EXECUTE old_asset_unique_stmt;
DEALLOCATE PREPARE old_asset_unique_stmt;

SET @asset_market_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_asset'
    AND index_name = 'idx_type_market_symbol'
);
SET @asset_market_index_sql := IF(@asset_market_index_exists = 0,
  'ALTER TABLE xo_asset ADD KEY idx_type_market_symbol (type, market, symbol)',
  'SELECT 1'
);
PREPARE asset_market_index_stmt FROM @asset_market_index_sql;
EXECUTE asset_market_index_stmt;
DEALLOCATE PREPARE asset_market_index_stmt;

SET @asset_market_unique_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_asset'
    AND index_name = 'uk_type_market_symbol_deleted'
);
SET @asset_market_unique_sql := IF(@asset_market_unique_exists = 0,
  'ALTER TABLE xo_asset ADD UNIQUE KEY uk_type_market_symbol_deleted (type, market, symbol, deleted)',
  'SELECT 1'
);
PREPARE asset_market_unique_stmt FROM @asset_market_unique_sql;
EXECUTE asset_market_unique_stmt;
DEALLOCATE PREPARE asset_market_unique_stmt;

ALTER TABLE xo_holding
  MODIFY quantity DECIMAL(28,10) NOT NULL DEFAULT 0 COMMENT '持仓数量，虚拟货币最多保留十位小数';

ALTER TABLE xo_investment_transaction
  MODIFY quantity DECIMAL(28,10) NOT NULL COMMENT '交易数量，虚拟货币最多保留十位小数';
