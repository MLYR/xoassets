-- 资产 market 与投资数量精度迁移脚本；已有本地库升级时在 schema.sql 后按需执行。
USE xoassets;

ALTER TABLE xo_asset
  ADD COLUMN IF NOT EXISTS market VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN' COMMENT '交易市场：SH SZ BJ US CN_FUND CRYPTO UNKNOWN' AFTER type;

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

ALTER TABLE xo_asset
  DROP INDEX uk_type_symbol_deleted,
  ADD KEY idx_type_market_symbol (type, market, symbol),
  ADD UNIQUE KEY uk_type_market_symbol_deleted (type, market, symbol, deleted);

ALTER TABLE xo_holding
  MODIFY quantity DECIMAL(28,10) NOT NULL DEFAULT 0 COMMENT '持仓数量，虚拟货币最多保留十位小数';

ALTER TABLE xo_investment_transaction
  MODIFY quantity DECIMAL(28,10) NOT NULL COMMENT '交易数量，虚拟货币最多保留十位小数';
