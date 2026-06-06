-- V5: 策略与回测
CREATE TABLE strategies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE COMMENT '策略名称',
    description     TEXT COMMENT '策略描述',
    strategy_type   VARCHAR(30) NOT NULL COMMENT '动量/均值回归/趋势跟踪/网格/自定义',
    parameters      JSON NOT NULL COMMENT '策略参数(JSON)',
    target_universe VARCHAR(20) COMMENT 'ETF/公募/全部',
    rebalance_freq  VARCHAR(20) COMMENT '每日/每周/每月/每季',
    max_holdings    INT DEFAULT 10 COMMENT '最大持仓数',
    version         INT DEFAULT 1 COMMENT '版本号',
    is_active       TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_strategy_type (strategy_type),
    INDEX idx_strategy_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略定义';

CREATE TABLE backtest_runs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_id     BIGINT NOT NULL,
    run_name        VARCHAR(200) COMMENT '运行名称',
    status          VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
    start_date      DATE NOT NULL COMMENT '回测起始',
    end_date        DATE NOT NULL COMMENT '回测结束',
    initial_capital DECIMAL(16,2) NOT NULL COMMENT '初始资金',
    commission_model VARCHAR(50) DEFAULT 'chinese_fund' COMMENT '费率模型',
    benchmark_id    BIGINT COMMENT '基准基金/指数ID',
    parameters_snapshot JSON COMMENT '参数快照(JSON)',
    started_at      TIMESTAMP NULL COMMENT '开始时间',
    completed_at    TIMESTAMP NULL COMMENT '完成时间',
    duration_seconds INT COMMENT '耗时(秒)',
    error_message   TEXT COMMENT '错误信息',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_backtest_strategy (strategy_id),
    INDEX idx_backtest_status (status),
    FOREIGN KEY (strategy_id) REFERENCES strategies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回测运行记录';

CREATE TABLE backtest_results (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    backtest_id     BIGINT NOT NULL UNIQUE,

    -- 整体表现
    total_return    DECIMAL(12,6) COMMENT '总收益率',
    annual_return   DECIMAL(12,6) COMMENT '年化收益率',
    volatility      DECIMAL(12,6) COMMENT '年化波动率',
    sharpe_ratio    DECIMAL(12,6) COMMENT '夏普比率',
    sortino_ratio   DECIMAL(12,6) COMMENT 'Sortino比率',
    calmar_ratio    DECIMAL(12,6) COMMENT 'Calmar比率',
    max_drawdown    DECIMAL(12,6) COMMENT '最大回撤',
    max_drawdown_start DATE COMMENT '回撤起始日',
    max_drawdown_end   DATE COMMENT '回撤结束日',
    win_rate        DECIMAL(8,4) COMMENT '胜率',
    total_trades    INT COMMENT '总交易次数',
    avg_hold_days   DECIMAL(8,2) COMMENT '平均持仓天数',

    -- 基准对比
    benchmark_return DECIMAL(12,6) COMMENT '基准收益',
    excess_return   DECIMAL(12,6) COMMENT '超额收益',
    tracking_error  DECIMAL(12,6) COMMENT '跟踪误差',
    information_ratio DECIMAL(12,6) COMMENT '信息比率',
    alpha           DECIMAL(12,6) COMMENT 'Alpha',
    beta            DECIMAL(12,6) COMMENT 'Beta',

    -- 详细数据
    monthly_returns JSON COMMENT '月度收益JSON',
    yearly_returns  JSON COMMENT '年度收益JSON',
    equity_curve    JSON COMMENT '净值曲线数据JSON',
    trade_log       JSON COMMENT '交易记录JSON',

    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (backtest_id) REFERENCES backtest_runs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回测结果';
