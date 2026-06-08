-- 账户余额修正、账户余额曲线和并发版本字段迁移。
USE xoassets;

SET @xo_account_version_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_account'
    AND column_name = 'version'
);
SET @xo_account_version_sql := IF(@xo_account_version_exists = 0,
  'ALTER TABLE xo_account ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE xo_account_version_stmt FROM @xo_account_version_sql;
EXECUTE xo_account_version_stmt;
DEALLOCATE PREPARE xo_account_version_stmt;

SET @xo_holding_version_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_holding'
    AND column_name = 'version'
);
SET @xo_holding_version_sql := IF(@xo_holding_version_exists = 0,
  'ALTER TABLE xo_holding ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''',
  'SELECT 1'
);
PREPARE xo_holding_version_stmt FROM @xo_holding_version_sql;
EXECUTE xo_holding_version_stmt;
DEALLOCATE PREPARE xo_holding_version_stmt;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'xo_transaction'
    AND index_name = 'idx_user_target_account_time'
);
SET @idx_sql := IF(@idx_exists = 0,
  'ALTER TABLE xo_transaction ADD KEY idx_user_target_account_time (user_id, target_account_id, transaction_time)',
  'SELECT 1'
);
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

CREATE TABLE IF NOT EXISTS xo_account_balance_adjustment (
  id BIGINT PRIMARY KEY COMMENT '修正ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  account_id BIGINT NOT NULL COMMENT '账户ID',
  before_balance DECIMAL(18,4) NOT NULL COMMENT '修正前余额',
  after_balance DECIMAL(18,4) NOT NULL COMMENT '修正后余额',
  delta_amount DECIMAL(18,4) NOT NULL COMMENT '修正差额',
  reason VARCHAR(255) DEFAULT NULL COMMENT '修正原因',
  operator_type VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '操作来源：USER SYSTEM ADMIN',
  biz_date DATE NOT NULL COMMENT '业务归属日期',
  biz_time DATETIME DEFAULT NULL COMMENT '业务发生时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_account_date (user_id, account_id, biz_date),
  KEY idx_account_created (account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户余额修正表';
