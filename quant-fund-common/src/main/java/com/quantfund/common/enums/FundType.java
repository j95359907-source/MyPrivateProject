package com.quantfund.common.enums;

/**
 * 基金类型枚举
 */
public enum FundType {
    ETF("ETF", "交易型开放式指数基金"),
    LOF("LOF", "上市型开放式基金"),
    OPEN_END("开放式", "普通开放式基金"),
    MONEY("货币", "货币市场基金"),
    CLOSED("封闭式", "封闭式基金"),
    QDII("QDII", "合格境内机构投资者基金"),
    FOF("FOF", "基金中的基金");

    private final String code;
    private final String description;

    FundType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
