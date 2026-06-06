package com.quantfund.core.portfolio;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 均值方差优化器 —— 基于 Apache Commons Math
 *
 * 求解二次规划问题：
 *   min  w^T Σ w  （最小化组合方差）
 *   s.t. w^T μ ≥ targetReturn （收益约束）
 *        Σ w_i = 1            （权重和为1）
 *        w_i ≥ 0              （禁止卖空）
 *
 * 核心方法：拉格朗日乘子法求解析解（无卖空约束时）
 *          或转为线性规划近似求解（有不等式约束时）
 */
public class MeanVarianceOptimizer implements PortfolioOptimizer {

    private static final int TRADING_DAYS = 250;

    @Override
    public OptimizationResult maxSharpe(double[][] returns, double riskFreeRate) {
        // 对于无卖空约束 + 风险资产，最大夏普组合的权重解析解为：
        // w* = Σ⁻¹(μ - rf) / sum(Σ⁻¹(μ - rf))
        int n = returns[0].length; // 资产数

        double[] meanReturns = calcAnnualizedReturns(returns);
        double[][] covMatrix = calcCovarianceMatrix(returns);

        // Σ⁻¹(μ - rf)
        RealMatrix sigmaInv = new LUDecomposition(
                new Array2DRowRealMatrix(covMatrix)).getSolver().getInverse();
        double[] excessReturns = new double[n];
        for (int i = 0; i < n; i++) {
            excessReturns[i] = meanReturns[i] - riskFreeRate;
        }
        RealVector excessVec = new ArrayRealVector(excessReturns);
        RealVector w = sigmaInv.operate(excessVec);

        // 归一化
        double sum = Arrays.stream(w.toArray()).sum();
        if (Math.abs(sum) < 1e-10) {
            return new OptimizationResult(new double[n], 0, 0, 0, "SINGULAR");
        }
        double[] weights = new double[n];
        for (int i = 0; i < n; i++) {
            weights[i] = w.getEntry(i) / sum;
        }

        // 裁剪负权重为0（处理卖空约束）
        clipNegative(weights);

        // 计算组合统计量
        double portReturn = dot(weights, meanReturns);
        double portVol = Math.sqrt(portfolioVariance(weights, covMatrix));
        double sharpe = portVol > 0 ? (portReturn - riskFreeRate) / portVol : 0;

        return new OptimizationResult(weights, portReturn, portVol, sharpe, "OK");
    }

    @Override
    public OptimizationResult minVolatility(double[][] returns) {
        int n = returns[0].length;
        double[][] covMatrix = calcCovarianceMatrix(returns);

        // 最小方差组合：Σ⁻¹·1 / (1^T·Σ⁻¹·1)
        RealMatrix sigmaInv = new LUDecomposition(
                new Array2DRowRealMatrix(covMatrix)).getSolver().getInverse();
        double[] ones = new double[n];
        Arrays.fill(ones, 1.0);
        RealVector onesVec = new ArrayRealVector(ones);
        RealVector w = sigmaInv.operate(onesVec);

        double sum = Arrays.stream(w.toArray()).sum();
        double[] weights = new double[n];
        for (int i = 0; i < n; i++) {
            weights[i] = w.getEntry(i) / sum;
        }
        clipNegative(weights);

        double[] meanReturns = calcAnnualizedReturns(returns);
        double portReturn = dot(weights, meanReturns);
        double portVol = Math.sqrt(portfolioVariance(weights, covMatrix));
        double sharpe = portVol > 0 ? (portReturn - 0.03) / portVol : 0;

        return new OptimizationResult(weights, portReturn, portVol, sharpe, "OK");
    }

    @Override
    public OptimizationResult riskParity(double[][] returns) {
        // 风险平价：使每个资产对组合总风险的边际贡献相等
        // RC_i = w_i * (Σw)_i / σ_p  （边际风险贡献）
        // 目标：RC_1 = RC_2 = ... = RC_n
        // 使用迭代算法求解

        int n = returns[0].length;
        double[][] covMatrix = calcCovarianceMatrix(returns);

        // 初始权重：等权
        double[] weights = new double[n];
        Arrays.fill(weights, 1.0 / n);

        // 迭代优化
        for (int iter = 0; iter < 50; iter++) {
            double[] rc = riskContribution(weights, covMatrix);
            double avgRC = Arrays.stream(rc).average().orElse(0);

            // 根据RC偏差调整权重
            double maxChange = 0;
            for (int i = 0; i < n; i++) {
                if (avgRC > 1e-10) {
                    double delta = (avgRC - rc[i]) / avgRC * 0.5;
                    weights[i] += delta / n;
                    maxChange = Math.max(maxChange, Math.abs(delta));
                }
            }

            // 归一化
            normalize(weights);
            if (maxChange < 1e-6) break;
        }

        clipNegative(weights);

        double[] meanReturns = calcAnnualizedReturns(returns);
        double portReturn = dot(weights, meanReturns);
        double portVol = Math.sqrt(portfolioVariance(weights, covMatrix));
        double sharpe = portVol > 0 ? (portReturn - 0.03) / portVol : 0;

        return new OptimizationResult(weights, portReturn, portVol, sharpe, "OK");
    }

    @Override
    public OptimizationResult efficientPortfolio(double[][] returns, double targetReturn) {
        // 给定目标收益，求最小方差组合
        // 解析解（无卖空约束）：
        // w = a·Σ⁻¹μ + b·Σ⁻¹1
        int n = returns[0].length;
        double[] mu = calcAnnualizedReturns(returns);
        double[][] sigma = calcCovarianceMatrix(returns);
        RealMatrix sigmaInv = new LUDecomposition(
                new Array2DRowRealMatrix(sigma)).getSolver().getInverse();

        double[] ones = new double[n]; Arrays.fill(ones, 1.0);
        RealVector wMu = sigmaInv.operate(new ArrayRealVector(mu));
        RealVector w1  = sigmaInv.operate(new ArrayRealVector(ones));

        double a = dot(wMu.toArray(), mu);
        double b = dot(wMu.toArray(), ones);
        double c = dot(w1.toArray(), ones);

        double det = a * c - b * b;
        if (Math.abs(det) < 1e-10) {
            return new OptimizationResult(new double[n], 0, 0, 0, "SINGULAR");
        }

        // 拉格朗日乘子法
        double lambda1 = (c * targetReturn - b) / det;
        double lambda2 = (a - b * targetReturn) / det;

        double[] weights = new double[n];
        for (int i = 0; i < n; i++) {
            weights[i] = lambda1 * wMu.getEntry(i) + lambda2 * w1.getEntry(i);
        }
        clipNegative(weights);

        double portReturn = dot(weights, mu);
        double portVol = Math.sqrt(portfolioVariance(weights, sigma));
        double sharpe = portVol > 0 ? (portReturn - 0.03) / portVol : 0;

        return new OptimizationResult(weights, portReturn, portVol, sharpe, "OK");
    }

    @Override
    public List<OptimizationResult> efficientFrontier(double[][] returns, int points) {
        double[] mu = calcAnnualizedReturns(returns);
        double minRet = Arrays.stream(mu).min().orElse(0);
        double maxRet = Arrays.stream(mu).max().orElse(0.5);

        List<OptimizationResult> frontier = new ArrayList<>();
        for (int i = 0; i <= points; i++) {
            double target = minRet + (maxRet - minRet) * i / points;
            OptimizationResult result = efficientPortfolio(returns, target);
            if ("OK".equals(result.status())) {
                frontier.add(result);
            }
        }
        return frontier;
    }

    // ========== 工具方法 ==========

    /** 从收益率矩阵计算年化收益向量 */
    private double[] calcAnnualizedReturns(double[][] returns) {
        int assets = returns[0].length;
        int periods = returns.length;
        double[] annual = new double[assets];
        for (int j = 0; j < assets; j++) {
            double product = 1.0;
            for (int i = 0; i < periods; i++) {
                product *= (1.0 + returns[i][j]);
            }
            double totalRet = product - 1.0;
            double years = (double) periods / TRADING_DAYS;
            annual[j] = years > 0 ? Math.pow(1.0 + totalRet, 1.0 / years) - 1.0 : 0;
        }
        return annual;
    }

    /** 计算协方差矩阵（年化） */
    private double[][] calcCovarianceMatrix(double[][] returns) {
        int assets = returns[0].length;
        Covariance cov = new Covariance();
        double[][] result = new double[assets][assets];
        for (int i = 0; i < assets; i++) {
            for (int j = 0; j < assets; j++) {
                double[] colI = getColumn(returns, i);
                double[] colJ = getColumn(returns, j);
                result[i][j] = cov.covariance(colI, colJ) * TRADING_DAYS;
            }
        }
        return result;
    }

    /** 组合方差 = w^T Σ w */
    double portfolioVariance(double[] weights, double[][] covMatrix) {
        double var = 0;
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights.length; j++) {
                var += weights[i] * weights[j] * covMatrix[i][j];
            }
        }
        return var;
    }

    /** 风险贡献向量 */
    private double[] riskContribution(double[] weights, double[][] covMatrix) {
        int n = weights.length;
        double portVar = portfolioVariance(weights, covMatrix);
        double portVol = Math.sqrt(portVar);

        double[] rc = new double[n];
        for (int i = 0; i < n; i++) {
            double marginal = 0;
            for (int j = 0; j < n; j++) {
                marginal += covMatrix[i][j] * weights[j];
            }
            rc[i] = portVol > 0 ? weights[i] * marginal / portVol : 0;
        }
        return rc;
    }

    /** 裁剪负权重为0并重新归一化 */
    void clipNegative(double[] weights) {
        boolean hasNegative = false;
        for (double w : weights) {
            if (w < 0) { hasNegative = true; break; }
        }
        if (hasNegative) {
            for (int i = 0; i < weights.length; i++) {
                if (weights[i] < 0) weights[i] = 0;
            }
            normalize(weights);
        }
    }

    void normalize(double[] weights) {
        double sum = Arrays.stream(weights).sum();
        if (sum > 0) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] /= sum;
            }
        }
    }

    private double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    private double[] getColumn(double[][] matrix, int col) {
        double[] colData = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) colData[i] = matrix[i][col];
        return colData;
    }
}
