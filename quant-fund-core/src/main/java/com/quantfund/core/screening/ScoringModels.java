package com.quantfund.core.screening;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 预置评分模型
 */
public final class ScoringModels {

    private ScoringModels() {}

    /** 按夏普比率排序（默认） */
    public static FundScreener.ScoringModel sharpeFirst() {
        return p -> p.metrics().getSharpe1y() != null
                ? p.metrics().getSharpe1y().doubleValue() * 100
                : -999;
    }

    /** 收益优先 */
    public static FundScreener.ScoringModel returnFirst() {
        return p -> p.metrics().getReturn1y() != null
                ? p.metrics().getReturn1y().doubleValue() * 100
                : -999;
    }

    /** 低回撤优先 */
    public static FundScreener.ScoringModel lowDrawdown() {
        return p -> {
            if (p.metrics().getMaxDrawdown1y() == null) return -999.0;
            return (1.0 + p.metrics().getMaxDrawdown1y().doubleValue()) * 100;
        };
    }

    /** 等权多因子综合评分
     *
     *  因子：收益(30%) + 夏普(25%) + 低回撤(20%) + Sortino(15%) + Calmar(10%)
     */
    public static FundScreener.ScoringModel multiFactor() {
        return p -> {
            var m = p.metrics();
            double score = 0;

            // 收益因子 (30%) —— z-score标准化
            if (m.getReturn1y() != null) score += 0.30 * m.getReturn1y().doubleValue();
            // 夏普因子 (25%)
            if (m.getSharpe1y() != null) score += 0.25 * m.getSharpe1y().doubleValue();
            // 低回撤因子 (20%) —— 回撤越小越好
            if (m.getMaxDrawdown1y() != null) score += 0.20 * (1.0 + m.getMaxDrawdown1y().doubleValue());
            // Sortino因子 (15%)
            if (m.getSortino1y() != null) score += 0.15 * m.getSortino1y().doubleValue();
            // Calmar因子 (10%)
            if (m.getCalmarRatio() != null) score += 0.10 * m.getCalmarRatio().doubleValue();

            return score * 100; // 放大便于阅读
        };
    }

    /**
     * 自定义权重多因子模型
     *
     * @param weights 因子权重 Map: "return" / "sharpe" / "drawdown" / "sortino" / "calmar"
     */
    public static FundScreener.ScoringModel customWeighted(Map<String, Double> weights) {
        return p -> {
            var m = p.metrics();
            double score = 0;
            score += weights.getOrDefault("return", 0.0)   * val(m.getReturn1y());
            score += weights.getOrDefault("sharpe", 0.0)    * val(m.getSharpe1y());
            score += weights.getOrDefault("drawdown", 0.0)  * (1.0 + val(m.getMaxDrawdown1y()));
            score += weights.getOrDefault("sortino", 0.0)   * val(m.getSortino1y());
            score += weights.getOrDefault("calmar", 0.0)    * val(m.getCalmarRatio());
            return score * 100;
        };
    }

    private static double val(BigDecimal bd) {
        return bd != null ? bd.doubleValue() : 0;
    }
}
