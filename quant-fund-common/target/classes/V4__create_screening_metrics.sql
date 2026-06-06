-- V4: 基金筛选指标（预计算表，每日更新）
CREATE TABLE screening_metrics (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_id         BIGINT NOT NULL,
    calc_date       DATE NOT NULL COMMENT '计算日期',

    -- 收益指标
    return_1m       DECIMAL(12,6) COMMENT '近1月收益',
    return_3m       DECIMAL(12,6) COMMENT '近3月收益',
    return_6m       DECIMAL(12,6) COMMENT '近6月收益',
    return_1y       DECIMAL(12,6) COMMENT '近1年收益',
    return_3y       DECIMAL(12,6) COMMENT '近3年年化',
    return_5y       DECIMAL(12,6) COMMENT '近5年年化',
    return_ytd      DECIMAL(12,6) COMMENT '年初至今',
    return_since_inception DECIMAL(12,6) COMMENT '成立以来',

    -- 风险指标
    volatility_1y   DECIMAL(12,6) COMMENT '年化波动率(1年)',
    volatility_3y   DECIMAL(12,6) COMMENT '年化波动率(3年)',
    max_drawdown_1y DECIMAL(12,6) COMMENT '最大回撤(1年)',
    max_drawdown_3y DECIMAL(12,6) COMMENT '最大回撤(3年)',
    downside_risk   DECIMAL(12,6) COMMENT '下行风险',

    -- 风险调整收益
    sharpe_1y       DECIMAL(12,6) COMMENT '夏普比率(1年)',
    sharpe_3y       DECIMAL(12,6) COMMENT '夏普比率(3年)',
    sortino_1y      DECIMAL(12,6) COMMENT 'Sortino比率(1年)',
    sortino_3y      DECIMAL(12,6) COMMENT 'Sortino比率(3年)',
    calmar_ratio    DECIMAL(12,6) COMMENT 'Calmar比率',
    information_ratio DECIMAL(12,6) COMMENT '信息比率',
    alpha           DECIMAL(12,6) COMMENT 'Jensen Alpha',
    beta            DECIMAL(12,6) COMMENT 'Beta',

    -- 其他
    tracking_error  DECIMAL(12,6) COMMENT '跟踪误差',
    win_rate        DECIMAL(8,4) COMMENT '月度胜率',
    avg_win         DECIMAL(12,6) COMMENT '平均盈利月',
    avg_loss        DECIMAL(12,6) COMMENT '平均亏损月',
    profit_months   INT COMMENT '盈利月份数',

    -- 同类排名
    rank_1y         INT COMMENT '同类排名(1年)',
    rank_3y         INT COMMENT '同类排名(3年)',
    percentile_1y   DECIMAL(6,2) COMMENT '百分比排名(1年)',

    data_source     VARCHAR(20),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_fund_date (fund_id, calc_date),
    INDEX idx_metrics_date (calc_date DESC),
    INDEX idx_sharpe_1y (sharpe_1y),
    INDEX idx_return_1y (return_1y),
    INDEX idx_max_drawdown_1y (max_drawdown_1y)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金筛选预计算指标';
