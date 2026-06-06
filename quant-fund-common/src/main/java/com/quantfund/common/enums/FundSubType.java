package com.quantfund.common.enums;

/**
 * 基金子类型枚举
 */
public enum FundSubType {
    INDEX("指数型", "被动跟踪指数"),
    STOCK("股票型", "主动管理股票基金"),
    MIXED("混合型", "股债混合"),
    BOND("债券型", "债券基金"),
    MONEY("货币型", "货币市场基金"),
    COMMODITY("商品型", "黄金/原油等商品基金"),
    REIT("REITs", "不动产投资信托"),
    OVERSEAS("海外型", "海外市场基金");

    private final String code;
    private final String description;

    FundSubType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
