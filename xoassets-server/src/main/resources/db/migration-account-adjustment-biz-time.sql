-- 账户余额修正支持业务发生时间，保留 biz_date 用于日统计和筛选。
USE xoassets;

SET @xo_adjustment_biz_time_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_account_balance_adjustment'
    AND column_name = 'biz_time'
);
SET @xo_adjustment_biz_time_sql := IF(@xo_adjustment_biz_time_exists = 0,
  'ALTER TABLE xo_account_balance_adjustment ADD COLUMN biz_time DATETIME DEFAULT NULL COMMENT ''业务发生时间'' AFTER biz_date',
  'SELECT 1'
);
PREPARE xo_adjustment_biz_time_stmt FROM @xo_adjustment_biz_time_sql;
EXECUTE xo_adjustment_biz_time_stmt;
DEALLOCATE PREPARE xo_adjustment_biz_time_stmt;

UPDATE xo_account_balance_adjustment
SET biz_time = TIMESTAMP(biz_date)
WHERE biz_time IS NULL;
