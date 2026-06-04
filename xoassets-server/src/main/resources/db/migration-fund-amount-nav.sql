-- 基金金额买入改造：支持待确认交易和确认净值 / 确认份额。
ALTER TABLE xo_investment_transaction
  ADD COLUMN IF NOT EXISTS input_mode VARCHAR(30) NOT NULL DEFAULT 'QUANTITY_PRICE' COMMENT '录入模式：QUANTITY_PRICE数量价格 AMOUNT_NAV金额净值' AFTER type,
  ADD COLUMN IF NOT EXISTS trade_amount DECIMAL(18,4) DEFAULT NULL COMMENT '用户录入交易总金额' AFTER input_mode,
  ADD COLUMN IF NOT EXISTS trade_quantity DECIMAL(28,10) DEFAULT NULL COMMENT '用户录入交易数量' AFTER trade_amount,
  ADD COLUMN IF NOT EXISTS trade_price DECIMAL(18,6) DEFAULT NULL COMMENT '用户录入成交价或净值' AFTER trade_quantity,
  ADD COLUMN IF NOT EXISTS trade_date DATE DEFAULT NULL COMMENT '交易发生日期' AFTER realized_profit,
  ADD COLUMN IF NOT EXISTS confirmed_date DATE DEFAULT NULL COMMENT '基金确认日期' AFTER trade_date,
  ADD COLUMN IF NOT EXISTS confirmed_nav DECIMAL(18,6) DEFAULT NULL COMMENT '基金确认单位净值' AFTER confirmed_date,
  ADD COLUMN IF NOT EXISTS confirmed_quantity DECIMAL(28,10) DEFAULT NULL COMMENT '基金确认份额' AFTER confirmed_nav;

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
