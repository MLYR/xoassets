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
  version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_id (user_id),
  KEY idx_user_status (user_id, status),
  KEY idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';

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
  image_url MEDIUMTEXT DEFAULT NULL COMMENT '流水图片，第一版保存上传后的图片地址或Data URL',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0作废',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_time (user_id, transaction_time),
  KEY idx_user_type_time (user_id, type, transaction_time),
  KEY idx_user_account_time (user_id, account_id, transaction_time),
  KEY idx_user_target_account_time (user_id, target_account_id, transaction_time),
  KEY idx_user_category_time (user_id, category_id, transaction_time),
  KEY idx_original_transaction (user_id, original_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支流水表';

CREATE TABLE IF NOT EXISTS xo_asset (
  id BIGINT PRIMARY KEY COMMENT '资产ID',
  symbol VARCHAR(80) NOT NULL COMMENT '资产代码',
  name VARCHAR(120) NOT NULL COMMENT '资产名称',
  type VARCHAR(20) NOT NULL COMMENT '资产类型：STOCK FUND CRYPTO OTHER',
  market VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN' COMMENT '交易市场：SH SZ BJ US CN_FUND CRYPTO UNKNOWN',
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '计价币种',
  quote_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL' COMMENT '行情来源：MANUAL COINGECKO EASTMONEY SINA YAHOO ALPHA_VANTAGE TUSHARE AKSHARE',
  quote_key VARCHAR(120) DEFAULT NULL COMMENT '外部行情查询键',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_type_symbol (type, symbol),
  KEY idx_type_market_symbol (type, market, symbol),
  KEY idx_name (name),
  UNIQUE KEY uk_type_market_symbol_deleted (type, market, symbol, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共资产基础表';

CREATE TABLE IF NOT EXISTS xo_holding (
  id BIGINT PRIMARY KEY COMMENT '持仓ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  asset_id BIGINT NOT NULL COMMENT '资产ID',
  quantity DECIMAL(28,10) NOT NULL DEFAULT 0 COMMENT '持仓数量，虚拟货币最多保留十位小数',
  avg_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '移动平均成本单价',
  total_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '持仓总成本',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
  version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_asset (user_id, asset_id),
  KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户投资持仓表';

CREATE TABLE IF NOT EXISTS xo_investment_transaction (
  id BIGINT PRIMARY KEY COMMENT '投资交易ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  holding_id BIGINT NOT NULL COMMENT '持仓ID',
  asset_id BIGINT NOT NULL COMMENT '资产ID',
  account_id BIGINT NOT NULL COMMENT '资金账户ID',
  type VARCHAR(20) NOT NULL COMMENT '交易类型：BUY SELL',
  input_mode VARCHAR(30) NOT NULL DEFAULT 'QUANTITY_PRICE' COMMENT '录入模式：QUANTITY_PRICE数量价格 AMOUNT_NAV金额净值',
  trade_amount DECIMAL(18,4) DEFAULT NULL COMMENT '用户录入交易总金额',
  trade_quantity DECIMAL(28,10) DEFAULT NULL COMMENT '用户录入交易数量',
  trade_price DECIMAL(18,6) DEFAULT NULL COMMENT '用户录入成交价或净值',
  quantity DECIMAL(28,10) NOT NULL COMMENT '交易数量，虚拟货币最多保留十位小数',
  price DECIMAL(18,4) NOT NULL COMMENT '成交单价',
  amount DECIMAL(18,4) NOT NULL COMMENT '成交金额',
  fee DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '手续费',
  cost_amount DECIMAL(18,4) DEFAULT NULL COMMENT '本次交易对应成本金额',
  realized_profit DECIMAL(18,4) DEFAULT NULL COMMENT '已实现盈亏',
  trade_date DATE DEFAULT NULL COMMENT '交易发生日期',
  confirmed_date DATE DEFAULT NULL COMMENT '基金确认日期',
  confirmed_nav DECIMAL(18,6) DEFAULT NULL COMMENT '基金确认单位净值',
  confirmed_quantity DECIMAL(28,10) DEFAULT NULL COMMENT '基金确认份额',
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL正常 CONFIRMED已确认 PENDING_CONFIRM待确认 FAILED失败 CANCELLED取消 REVOKED已撤销',
  revoke_time DATETIME DEFAULT NULL COMMENT '撤销时间',
  revoke_reason VARCHAR(255) DEFAULT NULL COMMENT '撤销原因',
  transaction_time DATETIME NOT NULL COMMENT '交易时间',
  note VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_time (user_id, transaction_time),
  KEY idx_user_holding_time (user_id, holding_id, transaction_time),
  KEY idx_user_asset_time (user_id, asset_id, transaction_time),
  KEY idx_user_account_time (user_id, account_id, transaction_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投资交易流水表';

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

CREATE TABLE IF NOT EXISTS xo_investment_daily_snapshot (
  id BIGINT PRIMARY KEY COMMENT 'ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  snapshot_date DATE NOT NULL COMMENT '快照日期',
  market_value DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '投资持仓市值',
  total_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '投资持仓成本',
  floating_profit DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '浮动盈亏',
  floating_profit_rate DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '浮动收益率',
  realized_profit DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '累计已实现盈亏',
  daily_profit DECIMAL(18,4) DEFAULT NULL COMMENT '快照日资金流调整收益：本日投资市值 - 上一快照日投资市值 - 当日投资本金净流入',
  daily_profit_rate DECIMAL(18,4) DEFAULT NULL COMMENT '快照日资金流调整收益率',
  net_inflow DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '当日投资本金净流入：买入为正，卖出为负',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  UNIQUE KEY uk_user_snapshot_date (user_id, snapshot_date, deleted),
  KEY idx_user_date (user_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户投资资产日快照表';

CREATE TABLE IF NOT EXISTS xo_budget (
  id BIGINT PRIMARY KEY COMMENT '预算ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  month VARCHAR(7) NOT NULL COMMENT '预算月份，格式 yyyy-MM',
  category_id BIGINT DEFAULT NULL COMMENT '分类ID，总预算为空',
  budget_type VARCHAR(20) NOT NULL COMMENT '预算类型：TOTAL CATEGORY',
  amount DECIMAL(18,4) NOT NULL COMMENT '预算金额',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_month (user_id, month),
  KEY idx_user_category_month (user_id, category_id, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算表';

CREATE TABLE IF NOT EXISTS xo_asset_snapshot (
  id BIGINT PRIMARY KEY COMMENT '资产快照ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  snapshot_date DATE NOT NULL COMMENT '快照日期',
  cash_asset DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '现金资产，正数账户余额合计',
  investment_asset DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '投资资产市值',
  total_asset DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '总资产：现金资产 + 投资资产',
  liability DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '负债，负数账户余额绝对值合计',
  net_asset DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '净资产：总资产 - 负债',
  investment_cost DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '投资持仓成本',
  investment_profit DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '投资浮动盈亏',
  investment_profit_rate DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '投资收益率百分比',
  monthly_income DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '截至快照日的当月普通收入',
  monthly_expense DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '截至快照日的当月普通支出',
  monthly_balance DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '截至快照日的当月结余',
  budget_used_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '截至快照日的预算已使用金额',
  budget_total_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '预算总金额',
  budget_usage_rate DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '预算使用率百分比',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  UNIQUE KEY uk_user_snapshot_date (user_id, snapshot_date, deleted),
  KEY idx_user_date (user_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资产快照表';

CREATE TABLE IF NOT EXISTS xo_snapshot_rebuild_task (
  id BIGINT PRIMARY KEY COMMENT '重建任务ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  start_date DATE NOT NULL COMMENT '重建开始日期',
  end_date DATE NOT NULL COMMENT '重建结束日期',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING RUNNING SUCCESS FAILED',
  trigger_type VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN' COMMENT '触发来源',
  error_message VARCHAR(255) DEFAULT NULL COMMENT '失败原因摘要',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  last_run_at DATETIME DEFAULT NULL COMMENT '最后执行时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_status_updated (status, updated_at),
  KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产快照重建任务表';

CREATE TABLE IF NOT EXISTS xo_goal (
  id BIGINT PRIMARY KEY COMMENT '目标ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  name VARCHAR(100) NOT NULL COMMENT '目标名称',
  target_amount DECIMAL(18,4) NOT NULL COMMENT '目标金额',
  current_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '当前金额',
  target_date DATE DEFAULT NULL COMMENT '目标日期',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE DONE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_status (user_id, status),
  KEY idx_user_target_date (user_id, target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产目标表';

CREATE TABLE IF NOT EXISTS xo_ai_report (
  id BIGINT PRIMARY KEY COMMENT '报告ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  report_type VARCHAR(20) NOT NULL COMMENT '报告类型：DAILY WEEKLY MONTHLY',
  report_date DATE NOT NULL COMMENT '报告日期',
  title VARCHAR(120) NOT NULL COMMENT '报告标题',
  content TEXT NOT NULL COMMENT '报告内容',
  summary_json TEXT DEFAULT NULL COMMENT '结构化摘要',
  status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS FAILED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  KEY idx_user_date (user_id, report_date),
  KEY idx_user_type_date (user_id, report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI财务报告表';

CREATE TABLE IF NOT EXISTS xo_market_calendar (
  id BIGINT PRIMARY KEY COMMENT '日历ID',
  market VARCHAR(30) NOT NULL COMMENT '市场：A_SHARE 港股/美股后续扩展',
  trade_date DATE NOT NULL COMMENT '日期',
  trading_day TINYINT(1) NOT NULL COMMENT '是否交易日：1是 0否',
  source VARCHAR(50) NOT NULL DEFAULT 'SYSTEM_WEEKDAY' COMMENT '来源：SYSTEM_WEEKDAY EXCHANGE_ANNOUNCEMENT MANUAL',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  UNIQUE KEY uk_market_date (market, trade_date, deleted),
  KEY idx_market_trading_day (market, trading_day, trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场交易日历表';
