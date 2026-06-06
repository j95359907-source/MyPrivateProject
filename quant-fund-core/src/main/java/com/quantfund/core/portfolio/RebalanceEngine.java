package com.quantfund.core.portfolio;

import java.util.*;

/**
 * 再平衡引擎 —— 生成调仓建议
 *
 * 两种模式：
 * 1. 阈值再平衡：当实际权重偏离目标权重超过阈值时触发
 * 2. 定期再平衡：每月/每季固定调仓
 */
public class RebalanceEngine {

    /**
     * 生成再平衡方案
     *
     * @param targetWeights 目标权重 Map<fundCode, weight>
     * @param currentWeights 当前权重 Map<fundCode, weight>
     * @param currentValue 当前总资产
     * @param threshold 触发阈值（例如0.05表示偏离5%时调整）
     */
    public RebalancePlan generatePlan(
            Map<String, Double> targetWeights,
            Map<String, Double> currentWeights,
            double currentValue,
            double threshold) {

        List<TradeInstruction> instructions = new ArrayList<>();
        Set<String> allFunds = new HashSet<>();
        allFunds.addAll(targetWeights.keySet());
        allFunds.addAll(currentWeights.keySet());

        boolean needsRebalance = false;

        for (String code : allFunds) {
            double target = targetWeights.getOrDefault(code, 0.0);
            double current = currentWeights.getOrDefault(code, 0.0);
            double deviation = target - current;

            // 检查是否触发阈值
            if (Math.abs(deviation) > threshold) {
                needsRebalance = true;

                if (deviation > 0) {
                    // 需要买入
                    double buyAmount = deviation * currentValue;
                    instructions.add(new TradeInstruction(
                            code, "BUY", buyAmount,
                            String.format("目标%.1f%% vs 实际%.1f%%，偏离%.1f%%",
                                    target * 100, current * 100, deviation * 100)));
                } else {
                    // 需要卖出
                    double sellAmount = Math.abs(deviation) * currentValue;
                    instructions.add(new TradeInstruction(
                            code, "SELL", sellAmount,
                            String.format("目标%.1f%% vs 实际%.1f%%，偏离%.1f%%",
                                    target * 100, current * 100, deviation * 100)));
                }
            }
        }

        // 处理不在目标中的持仓（全部卖出）
        for (String code : currentWeights.keySet()) {
            if (!targetWeights.containsKey(code) && currentWeights.get(code) > threshold) {
                needsRebalance = true;
                double sellAmount = currentWeights.get(code) * currentValue;
                instructions.add(new TradeInstruction(
                        code, "SELL", sellAmount, "已退出目标组合"));
            }
        }

        // 若新增基金
        for (String code : targetWeights.keySet()) {
            if (!currentWeights.containsKey(code) && targetWeights.get(code) > 0) {
                needsRebalance = true;
                double buyAmount = targetWeights.get(code) * currentValue;
                instructions.add(new TradeInstruction(
                        code, "BUY", buyAmount,
                        String.format("新增持仓，目标权重%.1f%%", targetWeights.get(code) * 100)));
            }
        }

        double totalBuy = instructions.stream()
                .filter(i -> "BUY".equals(i.type()))
                .mapToDouble(TradeInstruction::amount).sum();
        double totalSell = instructions.stream()
                .filter(i -> "SELL".equals(i.type()))
                .mapToDouble(TradeInstruction::amount).sum();

        return new RebalancePlan(needsRebalance, instructions, totalBuy, totalSell,
                totalBuy - totalSell, currentValue);
    }

    // --- 内部类型 ---

    public record TradeInstruction(
            String fundCode, String type, double amount, String reason
    ) {}

    public record RebalancePlan(
            boolean needsRebalance,
            List<TradeInstruction> instructions,
            double totalBuy, double totalSell,
            double netFlow, double currentValue
    ) {
        public boolean isBalanced() { return !needsRebalance && instructions.isEmpty(); }
    }
}
