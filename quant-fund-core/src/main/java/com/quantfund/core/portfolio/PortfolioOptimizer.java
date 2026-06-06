package com.quantfund.core.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 组合优化器接口
 */
public interface PortfolioOptimizer {

    /** 最大化夏普比率 */
    OptimizationResult maxSharpe(double[][] returns, double riskFreeRate);

    /** 最小化波动率 */
    OptimizationResult minVolatility(double[][] returns);

    /** 风险平价 */
    OptimizationResult riskParity(double[][] returns);

    /** 给定目标收益，最小化波动率 */
    OptimizationResult efficientPortfolio(double[][] returns, double targetReturn);

    /** 计算有效前沿 */
    List<OptimizationResult> efficientFrontier(double[][] returns, int points);

    /**
     * 优化结果
     */
    record OptimizationResult(
            double[] weights,        // 各资产权重
            double expectedReturn,   // 预期年化收益
            double volatility,       // 预期年化波动率
            double sharpeRatio,      // 夏普比率
            String status            // "OK" / "FAILED" / "SINGULAR"
    ) {}
}
