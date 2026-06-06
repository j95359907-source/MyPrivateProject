package com.quantfund.core.screening;

import java.math.BigDecimal;
import java.util.function.Predicate;

/**
 * 预置筛选条件 —— 链式Filter的原子构件
 */
public final class FundFilters {

    private FundFilters() {}

    /** 最低1年收益 */
    public static Predicate<FundScreener.FundMetricPair> minReturn1y(double min) {
        return p -> p.metrics().getReturn1y() != null
                && p.metrics().getReturn1y().doubleValue() >= min;
    }

    /** 最低3年收益 */
    public static Predicate<FundScreener.FundMetricPair> minReturn3y(double min) {
        return p -> p.metrics().getReturn3y() != null
                && p.metrics().getReturn3y().doubleValue() >= min;
    }

    /** 最大1年回撤（输入负值，如-0.15表示回撤不超过15%） */
    public static Predicate<FundScreener.FundMetricPair> maxDrawdown1y(double max) {
        return p -> p.metrics().getMaxDrawdown1y() != null
                && p.metrics().getMaxDrawdown1y().doubleValue() >= max;
    }

    /** 最低夏普比率 */
    public static Predicate<FundScreener.FundMetricPair> minSharpe(double min) {
        return p -> p.metrics().getSharpe1y() != null
                && p.metrics().getSharpe1y().doubleValue() >= min;
    }

    /** 最低Sortino比率 */
    public static Predicate<FundScreener.FundMetricPair> minSortino(double min) {
        return p -> p.metrics().getSortino1y() != null
                && p.metrics().getSortino1y().doubleValue() >= min;
    }

    /** 最高年化波动率 */
    public static Predicate<FundScreener.FundMetricPair> maxVolatility(double max) {
        return p -> p.metrics().getVolatility1y() != null
                && p.metrics().getVolatility1y().doubleValue() <= max;
    }

    /** 基金类型筛选 */
    public static Predicate<FundScreener.FundMetricPair> fundType(String type) {
        return p -> type.equals(p.fund().getFundType());
    }

    /** 仅ETF */
    public static Predicate<FundScreener.FundMetricPair> onlyEtf() {
        return p -> Boolean.TRUE.equals(p.fund().getIsEtf());
    }

    /** 仅指数基金 */
    public static Predicate<FundScreener.FundMetricPair> onlyIndex() {
        return p -> Boolean.TRUE.equals(p.fund().getIsIndex());
    }

    /** 基金规模范围（亿元） */
    public static Predicate<FundScreener.FundMetricPair> fundSizeBetween(double minBillion, double maxBillion) {
        BigDecimal min = BigDecimal.valueOf(minBillion * 1_0000_0000L);
        BigDecimal max = BigDecimal.valueOf(maxBillion * 1_0000_0000L);
        return p -> {
            if (p.fund().getFundSize() == null) return false;
            return p.fund().getFundSize().compareTo(min) >= 0
                    && p.fund().getFundSize().compareTo(max) <= 0;
        };
    }

    /** Beta范围（0.5到1.5表示市场中性到适度进攻） */
    public static Predicate<FundScreener.FundMetricPair> betaBetween(double min, double max) {
        return p -> p.metrics().getBeta() != null
                && p.metrics().getBeta().doubleValue() >= min
                && p.metrics().getBeta().doubleValue() <= max;
    }
}
