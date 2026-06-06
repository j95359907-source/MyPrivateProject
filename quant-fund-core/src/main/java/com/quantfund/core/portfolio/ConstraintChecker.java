package com.quantfund.core.portfolio;

import com.quantfund.common.entity.Fund;

import java.math.BigDecimal;
import java.util.*;

/**
 * 中国基金投资约束检查器
 */
public class ConstraintChecker {

    /** 默认约束 */
    private double minWeight = 0.05;    // 单只基金最低5%
    private double maxWeight = 0.30;    // 单只基金最高30%
    private int maxFunds = 10;          // 最多持仓10只
    private boolean allowEtf = true;
    private boolean allowPublicFund = true;

    public ConstraintChecker() {}

    public ConstraintChecker minWeight(double w) { this.minWeight = w; return this; }
    public ConstraintChecker maxWeight(double w) { this.maxWeight = w; return this; }
    public ConstraintChecker maxFunds(int n) { this.maxFunds = n; return this; }

    /**
     * 检查优化结果是否满足约束
     */
    public ConstraintResult check(double[] weights, List<Fund> funds) {
        List<String> violations = new ArrayList<>();

        // 1. 权重和为1
        double sum = Arrays.stream(weights).sum();
        if (Math.abs(sum - 1.0) > 0.01) {
            violations.add(String.format("权重和偏离: %.2f%%", (sum - 1) * 100));
        }

        // 2. 单只权重上下限
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] < -0.001) violations.add(funds.get(i).getFundCode() + " 负权重: " + weights[i]);
            if (weights[i] > maxWeight + 0.001) violations.add(funds.get(i).getFundCode() +
                    " 超上限: " + String.format("%.1f%%", weights[i] * 100));
        }

        // 3. 有效持仓数
        long effectiveCount = Arrays.stream(weights).filter(w -> w > 0.001).count();
        if (effectiveCount > maxFunds) {
            violations.add("持仓数" + effectiveCount + " > 上限" + maxFunds);
        }

        // 4. 最低持仓权重
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] > 0.001 && weights[i] < minWeight - 0.001) {
                violations.add(funds.get(i).getFundCode() +
                        " 过低: " + String.format("%.1f%%", weights[i] * 100) + " < " +
                        String.format("%.0f%%", minWeight * 100));
            }
        }

        // 5. A/C类互斥检查
        Map<String, Integer> fundBaseMap = new HashMap<>();
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] > 0.001) {
                String code = funds.get(i).getFundCode();
                String base = code.replaceAll("[AC]$", "");
                fundBaseMap.merge(base, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : fundBaseMap.entrySet()) {
            if (e.getValue() > 1) {
                violations.add("A/C类同时持有: " + e.getKey());
            }
        }

        return new ConstraintResult(violations.isEmpty(), violations);
    }

    /**
     * 应用约束后处理权重
     */
    public double[] applyConstraints(double[] rawWeights) {
        double[] adjusted = Arrays.copyOf(rawWeights, rawWeights.length);

        // 1. 剔除负值
        for (int i = 0; i < adjusted.length; i++) {
            if (adjusted[i] < 0) adjusted[i] = 0;
        }

        // 2. 剔除低于最低权重的小仓
        for (int i = 0; i < adjusted.length; i++) {
            if (adjusted[i] > 0 && adjusted[i] < minWeight) adjusted[i] = 0;
        }

        // 3. 限制最大权重
        for (int i = 0; i < adjusted.length; i++) {
            if (adjusted[i] > maxWeight) adjusted[i] = maxWeight;
        }

        // 4. 重新归一化
        double totalSum = Arrays.stream(adjusted).sum();
        if (totalSum > 0) {
            for (int i = 0; i < adjusted.length; i++) {
                adjusted[i] /= totalSum;
            }
        }

        return adjusted;
    }

    public record ConstraintResult(boolean valid, List<String> violations) {}
}
