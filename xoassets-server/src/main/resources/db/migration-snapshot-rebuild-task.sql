-- 资产快照重建任务表：补录历史流水时合并重建区间，由后台任务补跑历史快照。
USE xoassets;

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
