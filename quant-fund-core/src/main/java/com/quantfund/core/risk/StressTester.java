package com.quantfund.core.risk;

import java.math.BigDecimal;
import java.util.*;

/**
 * 压力测试引擎 —— 情景分析与最坏情况评估
 */
public class StressTester {

    /**
     * 预定义压力情景
     */
    public enum Scenario {
        MARKET_CRASH_2008("2008金融危机", -0.50),
        CHINA_2015("2015股灾", -0.40),
        COVID_2020("2020疫情", -0.30),
        RATE_HIKE("加息冲击", -0.15),
        CREDIT_CRISIS("信用危机", -0.25),
        MODERATE_DOWNTURN("温和下跌", -0.10);

        final String name;
        final double marketDrop;

        Scenario(String n, double d) { this.name = n; this.marketDrop = d; }
    }

    /**
     * 对组合持仓进行压力测试
     */
    public StressTestResult testPortfolio(
            Map<String, Double> weights,       // fundCode → weight
            Map<String, Double> betas,         // fundCode → beta
            double portfolioValue) {

        Map<String, Map<String, Double>> scenarioImpacts = new LinkedHashMap<>();

        for (Scenario scenario : Scenario.values()) {
            double totalLoss = 0;
            Map<String, Double> fundImpacts = new LinkedHashMap<>();

            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                String fund = entry.getKey();
                double weight = entry.getValue();
                double beta = betas.getOrDefault(fund, 1.0);
                double fundDrop = scenario.marketDrop * beta;
                double loss = portfolioValue * weight * fundDrop;
                totalLoss += loss;
                fundImpacts.put(fund, loss);
            }

            Map<String, Double> scenarioData = new LinkedHashMap<>();
            scenarioData.put("totalLoss", totalLoss);
            scenarioData.put("lossPct", totalLoss / portfolioValue);
            scenarioData.putAll(fundImpacts);
            scenarioImpacts.put(scenario.name, scenarioData);
        }

        // 最坏情景
        String worstScenario = scenarioImpacts.entrySet().stream()
                .min(Comparator.comparingDouble(e -> e.getValue().get("totalLoss")))
                .map(Map.Entry::getKey).orElse("N/A");
        double worstLoss = scenarioImpacts.get(worstScenario).get("totalLoss");

        return new StressTestResult(portfolioValue, scenarioImpacts, worstScenario, worstLoss);
    }

    /**
     * 蒙特卡洛模拟
     */
    public MonteCarloResult monteCarlo(
            double[] weights, double[][] covMatrix, double initialValue, int simulations, int days) {

        double[] annualReturns = new double[weights.length]; // 可从历史数据计算
        Random rand = new Random(42);

        double[][] paths = new double[simulations][days + 1];
        for (int s = 0; s < simulations; s++) {
            paths[s][0] = initialValue;
        }

        // Cholesky分解协方差矩阵生成相关随机数
        double[][] chol = cholesky(covMatrix);
        double[] finalValues = new double[simulations];

        for (int s = 0; s < simulations; s++) {
            double[] correlatedReturns = new double[weights.length];
            for (int i = 0; i < weights.length; i++) {
                double sum = 0;
                for (int j = 0; j <= i; j++) {
                    sum += chol[i][j] * rand.nextGaussian() / Math.sqrt(250);
                }
                correlatedReturns[i] = sum;
            }

            double dailyReturn = 0;
            for (int i = 0; i < weights.length; i++) {
                dailyReturn += weights[i] * correlatedReturns[i];
            }
            finalValues[s] = initialValue * Math.exp(dailyReturn * days);
        }

        Arrays.sort(finalValues);
        double var95 = finalValues[(int) (simulations * 0.05)] - initialValue;
        double var99 = finalValues[(int) (simulations * 0.01)] - initialValue;
        double expectedValue = Arrays.stream(finalValues).average().orElse(initialValue);

        return new MonteCarloResult(initialValue, expectedValue, var95, var99, simulations, days);
    }

    /** 简易Cholesky分解 */
    private double[][] cholesky(double[][] matrix) {
        int n = matrix.length;
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0;
                for (int k = 0; k < j; k++) sum += L[i][k] * L[j][k];
                if (i == j) L[i][j] = Math.sqrt(Math.max(matrix[i][i] - sum, 1e-10));
                else L[i][j] = (matrix[i][j] - sum) / L[j][j];
            }
        }
        return L;
    }

    // --- 结果类型 ---

    public record StressTestResult(double portfolioValue,
                                    Map<String, Map<String, Double>> scenarios,
                                    String worstScenario, double worstLoss) {}

    public record MonteCarloResult(double initialValue, double expectedValue,
                                    double var95, double var99, int simulations, int days) {}
}
