-- V1: 基金主表
CREATE TABLE funds (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_code       VARCHAR(10) NOT NULL COMMENT '6位基金代码',
    fund_name       VARCHAR(200) NOT NULL COMMENT '基金全称',
    fund_short_name VARCHAR(50) COMMENT '基金简称',
    fund_type       VARCHAR(30) NOT NULL COMMENT 'ETF/LOF/开放式/货币',
    fund_subtype    VARCHAR(30) COMMENT '指数型/股票型/混合型/债券型/货币型',

    share_class     VARCHAR(5) COMMENT 'A/C份额',
    is_etf          TINYINT(1) DEFAULT 0 COMMENT '是否ETF',
    is_index        TINYINT(1) DEFAULT 0 COMMENT '是否指数基金',
    tracking_index  VARCHAR(100) COMMENT '跟踪指数',

    management_company VARCHAR(200) COMMENT '基金公司',
    custodian_bank  VARCHAR(100) COMMENT '托管银行',
    inception_date  DATE COMMENT '成立日期',
    listing_date    DATE COMMENT '上市日期(ETF/LOF)',
    fund_size       DECIMAL(20,2) COMMENT '最新规模(元)',
    fund_size_date  DATE COMMENT '规模数据日期',

    management_fee  DECIMAL(6,4) COMMENT '管理费率(%)',
    custody_fee     DECIMAL(6,4) COMMENT '托管费率(%)',
    subscription_fee DECIMAL(6,4) COMMENT '申购费率(%)',
    redemption_fee  DECIMAL(6,4) COMMENT '赎回费率(%)',

    investment_target TEXT COMMENT '投资目标',
    benchmark       VARCHAR(500) COMMENT '业绩比较基准',
    sector_focus    VARCHAR(100) COMMENT '行业主题',

    is_active       TINYINT(1) DEFAULT 1 COMMENT '是否仍在运作',
    data_source     VARCHAR(20) COMMENT '数据来源: eastmoney/tushare',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_fund_code (fund_code),
    INDEX idx_fund_type (fund_type),
    INDEX idx_fund_subtype (fund_subtype),
    INDEX idx_company (management_company),
    INDEX idx_active_type (is_active, fund_type),
    INDEX idx_is_etf (is_etf),
    INDEX idx_is_index (is_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金主表';
