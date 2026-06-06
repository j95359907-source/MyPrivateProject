-- V7: 交易记录
CREATE TABLE trades (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id    BIGINT NOT NULL,
    fund_id         BIGINT NOT NULL,
    trade_type      VARCHAR(10) NOT NULL COMMENT 'BUY/SELL',
    trade_date      DATE NOT NULL COMMENT '交易日期',
    trade_time      TIMESTAMP COMMENT '精确交易时间',

    shares          DECIMAL(16,4) NOT NULL COMMENT '份额',
    price           DECIMAL(12,4) NOT NULL COMMENT '成交价(单位净值)',
    amount          DECIMAL(16,2) NOT NULL COMMENT '成交金额',
    commission      DECIMAL(12,4) DEFAULT 0 COMMENT '手续费',

    status          VARCHAR(20) DEFAULT 'EXECUTED' COMMENT 'PENDING/SUBMITTED/EXECUTED/FAILED/CANCELLED',
    execution_id    VARCHAR(100) COMMENT '券商订单ID',

    signal_source   VARCHAR(50) COMMENT '策略来源',
    strategy_id     BIGINT COMMENT '关联策略',
    signal_price    DECIMAL(12,4) COMMENT '信号价',
    signal_date     TIMESTAMP COMMENT '信号时间',

    notes           TEXT COMMENT '备注',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_trades_portfolio (portfolio_id, trade_date DESC),
    INDEX idx_trades_fund (fund_id),
    INDEX idx_trades_date (trade_date),
    INDEX idx_trades_status (status),
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id),
    FOREIGN KEY (fund_id) REFERENCES funds(id),
    FOREIGN KEY (strategy_id) REFERENCES strategies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录';
