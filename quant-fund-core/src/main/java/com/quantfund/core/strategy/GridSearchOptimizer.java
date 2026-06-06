package com.quantfund.core.strategy;

import com.quantfund.core.backtesting.BacktestEngine;
import com.quantfund.core.backtesting.BacktestResult;
import com.quantfund.core.backtesting.CommissionModel;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 策略参数优化器 —— 网格搜索 / 随机搜索
 *
 * 对策略参数空间进行搜索，找到最优参数组合
 */
public class GridSearchOptimizer {

    private final BacktestEngine engine;
    private final ExecutorService executor;

    public GridSearchOptimizer(int maxThreads) {
        this.engine = new BacktestEngine(CommissionModel.publicFund());
        this.executor = Executors.newFixedThreadPool(maxThreads);
    }

    /**
     * 网格搜索
     *
     * @param strategyFactory 策略工厂（接收参数，返回策略实例）
     * @param paramGrid       参数网格 Map<参数名, 候选值列表>
     * @param fundData        基金数据
     * @param startDate       回测起始
     * @param endDate         回测结束
     * @param capital         初始资金
     * @param scoreFunc       评分函数（如：按夏普比率评分）
     * @return 前N个最优结果
     */
    public List<SearchResult> gridSearch(
            Function<Map<String, Object>, Strategy> strategyFactory,
            Map<String, List<Object>> paramGrid,
            Map<String, Strategy.FundData> fundData,
            java.time.LocalDate startDate, java.time.LocalDate endDate,
            BigDecimal capital,
            Function<BacktestResult, Double> scoreFunc) {

        // 生成所有参数组合
        List<Map<String, Object>> allCombos = cartesianProduct(paramGrid);
        List<SearchResult> results = new ArrayList<>();

        // 并行执行回测
        List<Future<SearchResult>> futures = new ArrayList<>();
        for (Map<String, Object> params : allCombos) {
            futures.add(executor.submit(() -> {
                Strategy strategy = strategyFactory.apply(params);
                BacktestResult result = engine.run(strategy, fundData, startDate, endDate, capital);
                double score = scoreFunc.apply(result);
                return new SearchResult(params, result.getTotalReturn().doubleValue(),
                        result.getSharpeRatio() != null ? result.getSharpeRatio().doubleValue() : 0,
                        result.getMaxDrawdown() != null ? result.getMaxDrawdown().doubleValue() : 0,
                        score);
            }));
        }

        for (Future<SearchResult> f : futures) {
            try { results.add(f.get(5, TimeUnit.MINUTES)); }
            catch (Exception e) { /* 跳过失败的回测 */ }
        }

        results.sort(Comparator.comparingDouble(SearchResult::score).reversed());
        return results;
    }

    /**
     * 随机搜索 —— 在参数空间中随机采样
     */
    public List<SearchResult> randomSearch(
            Function<Map<String, Object>, Strategy> strategyFactory,
            Map<String, double[]> paramRanges, // 参数名 → [min, max]
            int samples,
            Map<String, Strategy.FundData> fundData,
            java.time.LocalDate startDate, java.time.LocalDate endDate,
            BigDecimal capital,
            Function<BacktestResult, Double> scoreFunc) {

        Random rand = new Random(42);
        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i < samples; i++) {
            Map<String, Object> params = new LinkedHashMap<>();
            for (Map.Entry<String, double[]> entry : paramRanges.entrySet()) {
                double[] range = entry.getValue();
                double val = range[0] + rand.nextDouble() * (range[1] - range[0]);
                if (val < 1) val = Math.round(val);
                params.put(entry.getKey(), (int) val);
            }

            Strategy strategy = strategyFactory.apply(params);
            BacktestResult result = engine.run(strategy, fundData, startDate, endDate, capital);
            double score = scoreFunc.apply(result);
            results.add(new SearchResult(params, result.getTotalReturn().doubleValue(),
                    result.getSharpeRatio() != null ? result.getSharpeRatio().doubleValue() : 0,
                    result.getMaxDrawdown() != null ? result.getMaxDrawdown().doubleValue() : 0,
                    score));
        }

        results.sort(Comparator.comparingDouble(SearchResult::score).reversed());
        return results;
    }

    /** 笛卡尔积 */
    private List<Map<String, Object>> cartesianProduct(Map<String, List<Object>> grid) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(new LinkedHashMap<>());

        for (Map.Entry<String, List<Object>> entry : grid.entrySet()) {
            List<Map<String, Object>> next = new ArrayList<>();
            for (Map<String, Object> combo : result) {
                for (Object val : entry.getValue()) {
                    Map<String, Object> newCombo = new LinkedHashMap<>(combo);
                    newCombo.put(entry.getKey(), val);
                    next.add(newCombo);
                }
            }
            result = next;
        }
        return result;
    }

    public void shutdown() { executor.shutdown(); }

    // --- 结果类型 ---

    public record SearchResult(Map<String, Object> params, double totalReturn,
                                double sharpe, double maxDrawdown, double score) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("params", params);
            m.put("totalReturn", totalReturn);
            m.put("sharpe", sharpe);
            m.put("maxDrawdown", maxDrawdown);
            m.put("score", score);
            return m;
        }
    }
}
