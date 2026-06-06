-- V2: 净值历史表（核心时间序列表 — 按年分区）
CREATE TABLE nav_history (
    id              BIGINT AUTO_INCREMENT,
    fund_id         BIGINT NOT NULL COMMENT '关联funds表',
    nav_date        DATE NOT NULL COMMENT '净值日期',

    unit_nav        DECIMAL(12,4) COMMENT '单位净值',
    acc_nav         DECIMAL(12,4) COMMENT '累计净值',
    daily_return    DECIMAL(10,6) COMMENT '日收益率',
    adj_nav         DECIMAL(12,4) COMMENT '复权净值(用于回测)',

    -- ETF特有字段
    iopv            DECIMAL(12,4) COMMENT '实时IOPV(参考净值)',
    premium_rate    DECIMAL(8,4) COMMENT '折溢价率(%)',
    etf_volume      BIGINT COMMENT 'ETF成交量(份)',
    etf_amount      DECIMAL(20,2) COMMENT 'ETF成交金额(元)',

    -- 分红和拆分
    dividend        DECIMAL(12,4) COMMENT '每份分红金额',
    split_ratio     DECIMAL(8,6) COMMENT '拆分比率',

    data_source     VARCHAR(20) COMMENT '数据来源',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id, nav_date),
    UNIQUE KEY uk_fund_date (fund_id, nav_date),
    INDEX idx_nav_fund_date (fund_id, nav_date DESC),
    INDEX idx_nav_date (nav_date),
    INDEX idx_nav_recent (fund_id, nav_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金净值历史'
PARTITION BY RANGE (YEAR(nav_date)) (
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
