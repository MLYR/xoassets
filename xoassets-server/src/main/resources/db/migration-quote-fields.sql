-- 行情统一模型兼容迁移：已有本地库执行本文件后，可继续保留旧价格快照。
ALTER TABLE xo_asset_price
  ADD COLUMN previous_close DECIMAL(28,8) DEFAULT NULL COMMENT '昨收价或上一交易日价格' AFTER currency,
  ADD COLUMN change_amount DECIMAL(28,8) DEFAULT NULL COMMENT '本次行情涨跌额' AFTER previous_close,
  ADD COLUMN change_percent DECIMAL(18,4) DEFAULT NULL COMMENT '本次行情涨跌幅百分比' AFTER change_amount,
  ADD COLUMN market_status VARCHAR(30) DEFAULT NULL COMMENT '市场状态：OPEN CLOSED UNKNOWN' AFTER quote_time;
