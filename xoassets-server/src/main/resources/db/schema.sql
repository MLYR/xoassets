-- XOAssets 小〇财迹第一期 MVP 表结构。
CREATE DATABASE IF NOT EXISTS xoassets DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE xoassets;

CREATE TABLE IF NOT EXISTS xo_user (
  id BIGINT PRIMARY KEY COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  UNIQUE KEY uk_username (username),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS xo_account (
  id BIGINT PRIMARY KEY COMMENT '账户ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  name VARCHAR(80) NOT NULL COMMENT '账户名称',
  type VARCHAR(30) NOT NULL COMMENT '账户类型：CASH BANK CREDITCARD ALIPAY WECHAT OTHER',
  balance DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '当前余额',
  initial_balance DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '初始余额',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_id (user_id),
  KEY idx_user_status (user_id, status),
  KEY idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

CREATE TABLE IF NOT EXISTS xo_category (
  id BIGINT PRIMARY KEY COMMENT '分类ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  name VARCHAR(50) NOT NULL COMMENT '分类名称',
  type VARCHAR(20) NOT NULL COMMENT '分类类型：INCOME EXPENSE',
  icon VARCHAR(50) DEFAULT NULL COMMENT '图标',
  color VARCHAR(20) DEFAULT NULL COMMENT '颜色',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_type (user_id, type),
  UNIQUE KEY uk_user_type_name (user_id, type, name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支分类表';

CREATE TABLE IF NOT EXISTS xo_transaction (
  id BIGINT PRIMARY KEY COMMENT '流水ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  type VARCHAR(20) NOT NULL COMMENT '流水类型：INCOME EXPENSE TRANSFER REFUND',
  amount DECIMAL(18,4) NOT NULL COMMENT '金额，统一正数',
  account_id BIGINT NOT NULL COMMENT '主账户ID',
  target_account_id BIGINT DEFAULT NULL COMMENT '目标账户ID，仅转账使用',
  category_id BIGINT DEFAULT NULL COMMENT '分类ID，转账可为空',
  original_transaction_id BIGINT DEFAULT NULL COMMENT '原流水ID，退款时使用',
  transaction_time DATETIME NOT NULL COMMENT '交易时间',
  note VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0作废',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_time (user_id, transaction_time),
  KEY idx_user_type_time (user_id, type, transaction_time),
  KEY idx_user_account_time (user_id, account_id, transaction_time),
  KEY idx_user_category_time (user_id, category_id, transaction_time),
  KEY idx_original_transaction (user_id, original_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支流水表';
