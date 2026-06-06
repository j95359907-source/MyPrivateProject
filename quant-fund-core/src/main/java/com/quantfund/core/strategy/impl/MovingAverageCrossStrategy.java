package com.quantfund.core.strategy.impl;

import com.quantfund.core.strategy.Strategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 双均线交叉策略 —— 短期均线上穿长期均线时买入，下穿时卖出
 *
 * 核心逻辑：
 * 1. 对每只基金计算短期均线(MA_fast)和长期均线(MA_slow)
 * 2. MA_fast > MA_slow → 持仓/买入（黄金交叉信号）
 * 3. MA_fast < MA_slow → 空仓/卖出（死亡交叉信号）
 *
 * 参数：
 *   - fastDays: 快线周期（默认20天）
 *   - slowDays: 慢线周期（默认60天）
 *   - topN: 最多持仓数（默认5只）
 *   - confirmDays: 确认天数（避免假突破，默认1天）
 */
public class MovingAverageCrossStrategy implements Strategy {

    private Map<String, Object> params = new LinkedHashMap<>();

    public MovingAverageCrossStrategy() {
        params.put("fastDays", 20);
        params.put("slowDays", 60);
        params.put("topN", 5);
        params.put("confirmDays", 1);
    }

    @Override public String getName() { return "双均线策略"; }
    @Override public String getType() { return "TREND"; }
    @Override public String getDescription() {
        return "MA" + params.get("fastDays") + "上穿MA" + params.get("slowDays") + "买入，下穿卖出";
    }

    @Override
    public List<Signal> generateSignals(StrategyContext context, Map<String, FundData> data) {
        int fastDays = (int) params.getOrDefault("fastDays", 20);
        int slowDays = (int) params.getOrDefault("slowDays", 60);
        int topN = (int) params.getOrDefault("topN", 5);

        List<Signal> signals = new ArrayList<>();

        // 1. 处理现有持仓
        for (Map.Entry<String, BigDecimal> holding : context.currentWeights().entrySet()) {
            String code = holding.getKey();
            FundData fd = data.get(code);
            if (fd == null) continue;

            CrossSignal cross = detectCross(fd, fastDays, slowDays);
            if (cross == CrossSignal.DEATH_CROSS) {
                signals.add(new Signal(code, SignalType.SELL, BigDecimal.ONE,
                        new BigDecimal("0.8"), "死亡交叉：MA" + fastDays + "下穿MA" + slowDays));
            }
        }

        // 2. 扫描金叉基金
        List<FundScore> goldCrossFunds = new ArrayList<>();
        for (Map.Entry<String, FundData> entry : data.entrySet()) {
            String code = entry.getKey();
            if (context.currentWeights().containsKey(code)) continue;

            FundData fd = entry.getValue();
            CrossSignal cross = detectCross(fd, fastDays, slowDays);
            if (cross == CrossSignal.GOLDEN_CROSS) {
                // 用近期涨幅作为排序依据
                BigDecimal recentReturn = calcRecentReturn(fd, fastDays);
                goldCrossFunds.add(new FundScore(code, recentReturn));
            }
        }

        goldCrossFunds.sort((a, b) -> b.score.compareTo(a.score));

        int currentHoldings = (int) context.currentWeights().size();
        long sellingCount = signals.stream().filter(s -> s.type() == SignalType.SELL).count();
        int availableSlots = Math.max(0, topN - currentHoldings + (int)sellingCount);

        BigDecimal equalWeight = BigDecimal.ONE.divide(
                BigDecimal.valueOf(Math.max(1, topN)), 4, RoundingMode.HALF_UP);

        for (int i = 0; i < Math.min(availableSlots, goldCrossFunds.size()); i++) {
            FundScore fs = goldCrossFunds.get(i);
            signals.add(new Signal(fs.fundCode, SignalType.BUY, equalWeight,
                    new BigDecimal("0.7"), "黄金交叉：MA" + fastDays + "上穿MA" + slowDays));
        }

        return signals;
    }

    /** 检测均线交叉信号 */
    private CrossSignal detectCross(FundData fd, int fastDays, int slowDays) {
        List<BigDecimal> navs = fd.navSeries();
        if (navs.size() < slowDays + 2) return CrossSignal.NONE;

        // 今天和昨天的快慢均线关系
        BigDecimal todayFast = ma(navs, navs.size() - 1, fastDays);
        BigDecimal todaySlow = ma(navs, navs.size() - 1, slowDays);
        BigDecimal yesterdayFast = ma(navs, navs.size() - 2, fastDays);
        BigDecimal yesterdaySlow = ma(navs, navs.size() - 2, slowDays);

        boolean todayAbove = todayFast.compareTo(todaySlow) > 0;
        boolean yesterdayAbove = yesterdayFast.compareTo(yesterdaySlow) > 0;

        if (todayAbove && !yesterdayAbove) return CrossSignal.GOLDEN_CROSS;
        if (!todayAbove && yesterdayAbove) return CrossSignal.DEATH_CROSS;
        return CrossSignal.NONE;
    }

    private BigDecimal ma(List<BigDecimal> navs, int endIdx, int days) {
        int start = Math.max(0, endIdx - days + 1);
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = start; i <= endIdx; i++) {
            sum = sum.add(navs.get(i));
        }
        return sum.divide(BigDecimal.valueOf(endIdx - start + 1), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calcRecentReturn(FundData fd, int days) {
        List<BigDecimal> returns = fd.returnSeries();
        int n = Math.min(days, returns.size());
        if (n == 0) return BigDecimal.ZERO;
        List<BigDecimal> window = returns.subList(Math.max(0, returns.size() - n), returns.size());
        BigDecimal product = BigDecimal.ONE;
        for (BigDecimal r : window) product = product.multiply(BigDecimal.ONE.add(r));
        return product.subtract(BigDecimal.ONE);
    }

    private enum CrossSignal { GOLDEN_CROSS, DEATH_CROSS, NONE }

    @Override public Map<String, Object> getParameters() { return params; }
    @Override public void setParameters(Map<String, Object> params) { this.params = new LinkedHashMap<>(params); }
    @Override public boolean validateParameters(Map<String, Object> params) {
        int fast = (int) params.getOrDefault("fastDays", 20);
        int slow = (int) params.getOrDefault("slowDays", 60);
        return fast > 0 && slow > fast;
    }

    private record FundScore(String fundCode, BigDecimal score) {}
}
