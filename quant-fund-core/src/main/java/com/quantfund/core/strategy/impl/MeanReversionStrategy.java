package com.quantfund.core.strategy.impl;

import com.quantfund.core.strategy.Strategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 均值回归策略 —— 买入近期超跌的基金，等待回归均值后卖出
 *
 * 核心逻辑：
 * 1. 计算每只基金相对其N日均线的偏离度
 * 2. 买入偏离度最低（最超跌）的基金
 * 3. 当偏离度恢复到阈值以上时卖出
 *
 * 参数：
 *   - maDays: 均线周期（默认60天）
 *   - buyThreshold: 买入偏离阈值（默认-5%，即低于均线5%时买入）
 *   - sellThreshold: 卖出偏离阈值（默认0%，即回归均线时卖出）
 *   - topN: 最多持仓数（默认5只）
 */
public class MeanReversionStrategy implements Strategy {

    private Map<String, Object> params = new LinkedHashMap<>();

    public MeanReversionStrategy() {
        params.put("maDays", 60);
        params.put("buyThreshold", -0.05);
        params.put("sellThreshold", 0.0);
        params.put("topN", 5);
    }

    @Override
    public String getName() { return "均值回归策略"; }

    @Override
    public String getType() { return "MEAN_REVERSION"; }

    @Override
    public String getDescription() {
        return "买入低于" + params.get("maDays") + "日均线" +
               String.format("%.0f%%", Math.abs((double)params.get("buyThreshold") * 100)) +
               "的基金，回归均线时卖出";
    }

    @Override
    public List<Signal> generateSignals(StrategyContext context, Map<String, FundData> data) {
        int maDays = (int) params.getOrDefault("maDays", 60);
        double buyThreshold = (double) params.getOrDefault("buyThreshold", -0.05);
        double sellThreshold = (double) params.getOrDefault("sellThreshold", 0.0);
        int topN = (int) params.getOrDefault("topN", 5);

        List<Signal> signals = new ArrayList<>();

        // 1. 处理现有持仓：检查是否需要卖出
        for (Map.Entry<String, BigDecimal> holding : context.currentWeights().entrySet()) {
            String code = holding.getKey();
            FundData fd = data.get(code);
            if (fd == null) continue;

            double deviation = calcDeviation(fd, maDays);
            if (deviation >= sellThreshold) {
                signals.add(new Signal(code, SignalType.SELL, BigDecimal.ONE,
                        new BigDecimal("0.8"), "偏离度恢复至" + String.format("%.1f%%", deviation * 100)));
            }
        }

        // 2. 扫描可买入的基金
        List<FundScore> oversold = new ArrayList<>();
        for (Map.Entry<String, FundData> entry : data.entrySet()) {
            String code = entry.getKey();
            FundData fd = entry.getValue();
            double deviation = calcDeviation(fd, maDays);
            if (deviation <= buyThreshold) {
                oversold.add(new FundScore(code, BigDecimal.valueOf(deviation)));
            }
        }

        // 按偏离度升序（越超跌越前面）
        oversold.sort(Comparator.comparing(a -> a.score));

        // 计算当前持仓数
        int currentHoldings = (int) context.currentWeights().size();
        int availableSlots = topN - currentHoldings;
        // 加上即将卖出的仓位
        long sellingCount = signals.stream().filter(s -> s.type() == SignalType.SELL).count();
        availableSlots += (int) sellingCount;

        BigDecimal equalWeight = BigDecimal.ONE.divide(
                BigDecimal.valueOf(Math.max(1, topN)), 4, RoundingMode.HALF_UP);

        for (int i = 0; i < Math.min(availableSlots, oversold.size()); i++) {
            FundScore fs = oversold.get(i);
            // 避免重复买入已持有的
            if (context.currentWeights().containsKey(fs.fundCode)) continue;

            signals.add(new Signal(fs.fundCode, SignalType.BUY, equalWeight,
                    new BigDecimal("0.75"),
                    "低偏离度" + String.format("%.1f%%", fs.score.doubleValue() * 100)));
        }

        return signals;
    }

    /** 计算当前价格相对N日均线的偏离度 */
    private double calcDeviation(FundData fd, int maDays) {
        List<BigDecimal> navs = fd.navSeries();
        if (navs.size() < maDays) return 0;

        BigDecimal current = navs.get(navs.size() - 1);
        BigDecimal sum = BigDecimal.ZERO;
        int start = navs.size() - maDays;
        for (int i = start; i < navs.size(); i++) {
            sum = sum.add(navs.get(i));
        }
        BigDecimal ma = sum.divide(BigDecimal.valueOf(maDays), 6, RoundingMode.HALF_UP);
        return current.subtract(ma).divide(ma, 6, RoundingMode.HALF_UP).doubleValue();
    }

    @Override public Map<String, Object> getParameters() { return params; }
    @Override public void setParameters(Map<String, Object> params) { this.params = new LinkedHashMap<>(params); }
    @Override public boolean validateParameters(Map<String, Object> params) {
        return (int) params.getOrDefault("maDays", 60) >= 5;
    }

    private record FundScore(String fundCode, BigDecimal score) {}
}
