package com.quantfund.core.strategy.impl;

import com.quantfund.core.strategy.Strategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 动量策略 —— 买入近期表现最强的N只基金，定期轮换
 *
 * 核心逻辑：
 * 1. 计算每只基金的回顾期收益率（默认12个月）
 * 2. 按收益率排序，买入Top-N只
 * 3. 每M个月（默认1个月）重新排名，换仓
 *
 * 参数：
 *   - lookbackMonths: 动量回顾期（默认12个月）
 *   - topN: 持仓数量（默认5只）
 *   - rebalanceMonths: 调仓频率（默认1个月）
 *   - minReturn: 最低动量阈值（默认0，即只要正收益）
 */
public class MomentumStrategy implements Strategy {

    private Map<String, Object> params = new LinkedHashMap<>();

    public MomentumStrategy() {
        params.put("lookbackMonths", 12);
        params.put("topN", 5);
        params.put("rebalanceMonths", 1);
        params.put("minReturn", 0.0);
    }

    @Override
    public String getName() { return "动量轮动策略"; }

    @Override
    public String getType() { return "MOMENTUM"; }

    @Override
    public String getDescription() {
        return "买入过去" + params.get("lookbackMonths") + "个月表现最好的" +
               params.get("topN") + "只基金，每月调仓一次";
    }

    @Override
    public List<Signal> generateSignals(StrategyContext context, Map<String, FundData> data) {
        int lookbackMonths = (int) params.getOrDefault("lookbackMonths", 12);
        int topN = (int) params.getOrDefault("topN", 5);
        double minReturn = (double) params.getOrDefault("minReturn", 0.0);
        int lookbackDays = lookbackMonths * 21; // 约21个交易日/月

        // 1. 计算每只基金的动量得分（回顾期收益率）
        List<FundScore> scores = new ArrayList<>();
        for (Map.Entry<String, FundData> entry : data.entrySet()) {
            FundData fd = entry.getValue();
            BigDecimal momentum = calcMomentum(fd.returnSeries(), lookbackDays);
            if (momentum.doubleValue() >= minReturn) {
                scores.add(new FundScore(entry.getKey(), momentum));
            }
        }

        // 2. 按动量排序
        scores.sort((a, b) -> b.score.compareTo(a.score));

        // 3. 选TopN
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, scores.size()); i++) {
            selected.add(scores.get(i).fundCode);
        }

        // 4. 生成信号
        List<Signal> signals = new ArrayList<>();
        BigDecimal equalWeight = BigDecimal.ONE.divide(
                BigDecimal.valueOf(topN), 4, RoundingMode.HALF_UP);

        // 对未持有的TopN基金发出BUY
        for (String code : selected) {
            if (!context.currentWeights().containsKey(code) ||
                context.currentWeights().get(code).compareTo(equalWeight.subtract(new BigDecimal("0.05"))) < 0) {
                signals.add(new Signal(code, SignalType.BUY, equalWeight,
                        new BigDecimal("0.7"), "动量排名Top" + topN));
            }
        }

        // 对持有但不在TopN的基金发出SELL
        for (String code : context.currentWeights().keySet()) {
            if (!selected.contains(code)) {
                signals.add(new Signal(code, SignalType.SELL, BigDecimal.ONE,
                        new BigDecimal("0.8"), "跌出Top" + topN));
            }
        }

        return signals;
    }

    /** 计算指定回看期的累计收益 */
    private BigDecimal calcMomentum(List<BigDecimal> dailyReturns, int lookbackDays) {
        int n = Math.min(lookbackDays, dailyReturns.size());
        if (n == 0) return BigDecimal.ZERO;
        List<BigDecimal> window = dailyReturns.subList(
                Math.max(0, dailyReturns.size() - n), dailyReturns.size());
        BigDecimal product = BigDecimal.ONE;
        for (BigDecimal r : window) {
            product = product.multiply(BigDecimal.ONE.add(r));
        }
        return product.subtract(BigDecimal.ONE);
    }

    @Override
    public Map<String, Object> getParameters() { return params; }

    @Override
    public void setParameters(Map<String, Object> params) { this.params = new LinkedHashMap<>(params); }

    @Override
    public boolean validateParameters(Map<String, Object> params) {
        int topN = (int) params.getOrDefault("topN", 5);
        return topN >= 1 && topN <= 20;
    }

    private record FundScore(String fundCode, BigDecimal score) {}
}
