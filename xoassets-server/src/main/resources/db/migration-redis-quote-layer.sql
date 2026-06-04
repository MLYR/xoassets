-- 投资行情分层改造：MySQL 存当前价/日级价/用户投资日快照，Redis 存短期原始快照。
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
  daily_profit DECIMAL(18,4) DEFAULT NULL COMMENT '当日收益',
  daily_profit_rate DECIMAL(18,4) DEFAULT NULL COMMENT '当日收益率',
  net_inflow DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '当日净入金',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  UNIQUE KEY uk_user_snapshot_date (user_id, snapshot_date, deleted),
  KEY idx_user_date (user_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户投资资产日快照表';
