-- V6: 投资组合
CREATE TABLE portfolios (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT '组合名称',
    description     TEXT COMMENT '组合描述',
    portfolio_type  VARCHAR(20) DEFAULT 'CUSTOM' COMMENT 'CUSTOM/OPTIMIZED/STRATEGY_DRIVEN',
    initial_capital DECIMAL(16,2) COMMENT '初始资金',
    current_value   DECIMAL(16,2) COMMENT '最新估值',
    cash_balance    DECIMAL(16,2) DEFAULT 0 COMMENT '现金余额',
    risk_free_rate  DECIMAL(6,4) DEFAULT 0.0200 COMMENT '无风险利率',
    target_volatility DECIMAL(6,4) COMMENT '目标波动率',
    rebalance_freq  VARCHAR(20) DEFAULT 'MONTHLY' COMMENT '再平衡频率',
    last_rebalanced DATE COMMENT '上次再平衡日期',
    is_active       TINYINT(1) DEFAULT 1 COMMENT '是否活跃',
    strategy_id     BIGINT COMMENT '关联策略ID',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_portfolio_active (is_active),
    FOREIGN KEY (strategy_id) REFERENCES strategies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投资组合';

CREATE TABLE portfolio_holdings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id    BIGINT NOT NULL,
    fund_id         BIGINT NOT NULL,

    target_weight   DECIMAL(8,4) NOT NULL COMMENT '目标权重(%)',
    current_weight  DECIMAL(8,4) COMMENT '当前实际权重(%)',

    shares_held     DECIMAL(16,4) COMMENT '持有份额',
    avg_cost        DECIMAL(12,4) COMMENT '平均成本',
    current_value   DECIMAL(16,2) COMMENT '当前市值',
    unrealized_pnl  DECIMAL(12,2) COMMENT '未实现盈亏',
    total_return    DECIMAL(12,6) COMMENT '总收益率',

    is_active       TINYINT(1) DEFAULT 1 COMMENT '是否当前持仓',
    entry_date      DATE NOT NULL COMMENT '建仓日期',
    exit_date       DATE COMMENT '清仓日期',

    UNIQUE KEY uk_portfolio_fund_entry (portfolio_id, fund_id, entry_date),
    INDEX idx_holdings_portfolio (portfolio_id),
    INDEX idx_holdings_active (portfolio_id, is_active),
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    FOREIGN KEY (fund_id) REFERENCES funds(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组合持仓';
