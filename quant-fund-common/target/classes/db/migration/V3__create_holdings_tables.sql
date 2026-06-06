-- V3: 基金持仓 + 基金经理
CREATE TABLE fund_holdings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_id         BIGINT NOT NULL,
    report_date     DATE NOT NULL COMMENT '报告期',
    period_type     VARCHAR(10) COMMENT 'Q1/Q2/Q3/Q4/中期/年度',

    stock_code      VARCHAR(10) COMMENT '股票代码',
    stock_name      VARCHAR(100) COMMENT '股票名称',
    shares_held     DECIMAL(16,2) COMMENT '持股数量',
    market_value    DECIMAL(20,2) COMMENT '持仓市值(元)',
    pct_of_nav      DECIMAL(8,4) COMMENT '占净值比例(%)',

    sector_code     VARCHAR(20) COMMENT '行业代码',
    sector_name     VARCHAR(100) COMMENT '行业名称',
    sector_pct      DECIMAL(8,4) COMMENT '行业配置比例(%)',

    data_source     VARCHAR(20),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_fund_report_stock (fund_id, report_date, stock_code),
    INDEX idx_holdings_fund (fund_id, report_date DESC),
    INDEX idx_holdings_stock (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金持仓明细';

-- 基金经理
CREATE TABLE fund_managers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    manager_name    VARCHAR(50) NOT NULL COMMENT '姓名',
    manager_code    VARCHAR(20) UNIQUE COMMENT '人物代码',

    gender          VARCHAR(5) COMMENT '性别',
    education       VARCHAR(50) COMMENT '最高学历',
    grad_school     VARCHAR(100) COMMENT '毕业院校',
    qualification   VARCHAR(50) COMMENT '从业资格',

    current_company VARCHAR(200) COMMENT '现任公司',
    start_date      DATE COMMENT '从业起始日期',
    tenure_years    DECIMAL(4,1) COMMENT '证券从业年限',
    avg_tenure_current_fund DECIMAL(4,1) COMMENT '现任基金经理平均任职年限',

    fund_count      INT COMMENT '管理基金数',
    best_return     DECIMAL(8,4) COMMENT '任期最佳回报',
    worst_return    DECIMAL(8,4) COMMENT '任期最差回报',

    data_source     VARCHAR(20),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金经理';

-- 基金经理-基金关联表
CREATE TABLE fund_manager_assignments (
    fund_id         BIGINT NOT NULL,
    manager_id      BIGINT NOT NULL,
    start_date      DATE NOT NULL COMMENT '任职起始',
    end_date        DATE COMMENT '任职结束(NULL=现任)',
    is_current      TINYINT(1) DEFAULT 1 COMMENT '是否现任',

    PRIMARY KEY (fund_id, manager_id, start_date),
    INDEX idx_manager_fund (manager_id),
    INDEX idx_current (is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金经理任职记录';

-- 基准指数
CREATE TABLE reference_indices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    index_code      VARCHAR(20) UNIQUE NOT NULL COMMENT '000300/000905等',
    index_name      VARCHAR(200) NOT NULL COMMENT '指数名称',
    index_type      VARCHAR(30) COMMENT '股票/债券/商品',
    data_source     VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基准指数';

-- 指数日行情
CREATE TABLE index_daily (
    id              BIGINT AUTO_INCREMENT,
    index_id        BIGINT NOT NULL,
    trade_date      DATE NOT NULL,
    close_price     DECIMAL(12,4),
    open_price      DECIMAL(12,4),
    high_price      DECIMAL(12,4),
    low_price       DECIMAL(12,4),
    volume          BIGINT COMMENT '成交量',
    amount          DECIMAL(20,2) COMMENT '成交额',
    daily_return    DECIMAL(10,6) COMMENT '日收益率',

    PRIMARY KEY (id, trade_date),
    UNIQUE KEY uk_index_date (index_id, trade_date),
    INDEX idx_index_date (index_id, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指数日行情'
PARTITION BY RANGE (YEAR(trade_date)) (
    PARTITION p2015 VALUES LESS THAN (2016),
    PARTITION p2016 VALUES LESS THAN (2017),
    PARTITION p2017 VALUES LESS THAN (2018),
    PARTITION p2018 VALUES LESS THAN (2019),
    PARTITION p2019 VALUES LESS THAN (2020),
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
