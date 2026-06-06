package com.quantfund.core.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 风险计算器 —— VaR / CVaR / 压力测试 / 相关性矩阵
 */
public class RiskCalculator {

    private static final int TRADING_DAYS = 250;

    /** 历史模拟法 VaR（95%置信度） */
    public static double historicalVaR(List<BigDecimal> dailyReturns, double confidence) {
        List<Double> sorted = dailyReturns.stream()
                .mapToDouble(BigDecimal::doubleValue).sorted().boxed().toList();
        int idx = (int) ((1.0 - confidence) * sorted.size());
        if (idx >= sorted.size()) idx = sorted.size() - 1;
        return sorted.get(idx);
    }

    /** 参数法 VaR（假设正态分布） */
    public static double parametricVaR(double annualReturn, double annualVolatility,
                                        double confidence, double investment) {
        // VaR = μ + σ * z_α  (z_α for 95% = -1.645, 99% = -2.326)
        double zScore = confidence >= 0.99 ? -2.326 : -1.645;
        double dailyVol = annualVolatility / Math.sqrt(TRADING_DAYS);
        double dailyReturn = annualReturn / TRADING_DAYS;
        double dailyVaR = dailyReturn + dailyVol * zScore;
        return dailyVaR * investment;
    }

    /** CVaR（条件风险价值 / Expected Shortfall） */
    public static double cvar(List<BigDecimal> dailyReturns, double confidence) {
        double var = historicalVaR(dailyReturns, confidence);
        List<Double> below = dailyReturns.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .filter(r -> r <= var).boxed().toList();
        return below.stream().mapToDouble(Double::doubleValue).average().orElse(var);
    }

    /** 计算相关系数矩阵 */
    public static double[][] correlationMatrix(List<List<BigDecimal>> returnSeries) {
        int n = returnSeries.size();
        double[][] corr = new double[n][n];

        for (int i = 0; i < n; i++) {
            corr[i][i] = 1.0;
            for (int j = i + 1; j < n; j++) {
                double c = pearsonCorrelation(returnSeries.get(i), returnSeries.get(j));
                corr[i][j] = c;
                corr[j][i] = c;
            }
        }
        return corr;
    }

    /** 皮尔逊相关系数 */
    public static double pearsonCorrelation(List<BigDecimal> x, List<BigDecimal> y) {
        int n = Math.min(x.size(), y.size());
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        for (int i = 0; i < n; i++) {
            double xi = x.get(i).doubleValue();
            double yi = y.get(i).doubleValue();
            sumX += xi; sumY += yi; sumXY += xi * yi;
            sumX2 += xi * xi; sumY2 += yi * yi;
        }
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        return denominator > 0 ? numerator / denominator : 0;
    }

    /** 计算滚动夏普比率 */
    public static List<Double> rollingSharpe(List<BigDecimal> dailyReturns, int window, double riskFreeRate) {
        List<Double> result = new ArrayList<>();
        for (int i = window; i <= dailyReturns.size(); i++) {
            List<BigDecimal> sub = dailyReturns.subList(i - window, i);
            double avg = sub.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            double variance = sub.stream()
                    .mapToDouble(r -> Math.pow(r.doubleValue() - avg, 2)).average().orElse(0);
            double annualReturn = avg * TRADING_DAYS;
            double annualVol = Math.sqrt(variance) * Math.sqrt(TRADING_DAYS);
            double sharpe = annualVol > 0 ? (annualReturn - riskFreeRate) / annualVol : 0;
            result.add(sharpe);
        }
        return result;
    }
}
