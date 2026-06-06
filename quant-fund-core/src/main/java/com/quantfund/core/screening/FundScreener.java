package com.quantfund.core.screening;

import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.ScreeningMetrics;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 基金筛选引擎 —— 链式Filter + 多因子复合评分
 *
 * 使用方式：
 *   List<ScoredFund> results = new FundScreener()
 *       .addFilter(FundFilters.minReturn1y(0.10))
 *       .addFilter(FundFilters.maxDrawdown1y(-0.15))
 *       .addFilter(FundFilters.minSharpe(1.0))
 *       .scoreWith(ScoringModels.EQUAL_WEIGHT)
 *       .rank(20);
 */
public class FundScreener {

    private final List<Predicate<FundMetricPair>> filters = new ArrayList<>();
    private ScoringModel scoringModel = ScoringModels.sharpeFirst(); // 默认按夏普排序

    /** 添加筛选条件 */
    public FundScreener addFilter(Predicate<FundMetricPair> filter) {
        filters.add(filter);
        return this;
    }

    /** 设置评分模型 */
    public FundScreener scoreWith(ScoringModel model) {
        this.scoringModel = model;
        return this;
    }

    /** 执行筛选并返回TopN */
    public List<ScoredFund> rank(List<FundMetricPair> candidates, int topN) {
        // 1. 链式过滤
        List<FundMetricPair> filtered = candidates.stream()
                .filter(pair -> filters.stream().allMatch(f -> f.test(pair)))
                .toList();

        // 2. 评分
        List<ScoredFund> scored = new ArrayList<>();
        for (FundMetricPair pair : filtered) {
            double score = scoringModel.score(pair);
            scored.add(new ScoredFund(pair.fund, pair.metrics, score));
        }

        // 3. 排序 + TopN
        scored.sort(Comparator.comparingDouble(ScoredFund::score).reversed());
        return scored.stream().limit(topN).toList();
    }

    // ============ 内部类型 ============

    public record FundMetricPair(Fund fund, ScreeningMetrics metrics) {}

    public record ScoredFund(Fund fund, ScreeningMetrics metrics, double score) {}

    /** 评分模型接口 */
    @FunctionalInterface
    public interface ScoringModel {
        double score(FundMetricPair pair);
    }
}
