package com.quantfund.api.service;

import com.quantfund.common.entity.*;
import com.quantfund.common.repository.*;
import com.quantfund.core.portfolio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    @Autowired private PortfolioRepository portfolioRepo;
    @Autowired private PortfolioHoldingRepository holdingRepo;
    @Autowired private FundRepository fundRepo;
    @Autowired private NavHistoryRepository navRepo;

    private final MeanVarianceOptimizer optimizer = new MeanVarianceOptimizer();
    private final ConstraintChecker checker = new ConstraintChecker();
    private final RebalanceEngine rebalancer = new RebalanceEngine();

    @Transactional
    public Portfolio createPortfolio(String name, String description, BigDecimal initialCapital) {
        Portfolio p = new Portfolio();
        p.setName(name);
        p.setDescription(description);
        p.setInitialCapital(initialCapital);
        p.setCurrentValue(initialCapital);
        p.setCashBalance(initialCapital);
        return portfolioRepo.save(p);
    }

    @Transactional
    public void addFundToPortfolio(Long portfolioId, String fundCode, BigDecimal targetWeight) {
        Portfolio portfolio = portfolioRepo.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("组合不存在"));
        Fund fund = fundRepo.findByFundCode(fundCode)
                .orElseThrow(() -> new IllegalArgumentException("基金不存在"));

        PortfolioHolding holding = new PortfolioHolding();
        holding.setPortfolioId(portfolioId);
        holding.setFundId(fund.getId());
        holding.setTargetWeight(targetWeight);
        holding.setCurrentWeight(BigDecimal.ZERO);
        holding.setEntryDate(LocalDate.now());
        holding.setIsActive(true);
        holdingRepo.save(holding);
    }

    /**
     * 运行组合优化
     */
    public Map<String, Object> optimize(Long portfolioId, String objective) {
        Portfolio portfolio = portfolioRepo.findById(portfolioId).orElseThrow();
        List<PortfolioHolding> holdings = holdingRepo.findByPortfolioIdAndIsActiveTrue(portfolioId);

        if (holdings.size() < 2) {
            return Map.of("error", "至少需要2只基金进行优化");
        }

        // 加载历史收益数据
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(3);
        List<String> fundCodes = new ArrayList<>();

        double[][] returns = loadReturnMatrix(holdings, startDate, endDate, fundCodes);
        if (returns == null || returns.length < 60) {
            return Map.of("error", "历史数据不足（需要至少60个交易日）");
        }

        // 运行优化
        PortfolioOptimizer.OptimizationResult result = switch (objective) {
            case "min_volatility" -> optimizer.minVolatility(returns);
            case "risk_parity" -> optimizer.riskParity(returns);
            default -> optimizer.maxSharpe(returns, 0.03);
        };

        // 应用约束
        double[] finalWeights = checker.applyConstraints(result.weights());

        // 计算有效前沿
        List<PortfolioOptimizer.OptimizationResult> frontier = optimizer.efficientFrontier(returns, 15);

        // 构建响应
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("objective", objective);
        response.put("weights", buildWeightMap(fundCodes, finalWeights));
        response.put("expectedReturn", result.expectedReturn());
        response.put("volatility", result.volatility());
        response.put("sharpeRatio", result.sharpeRatio());
        response.put("status", result.status());

        // 有效前沿数据
        List<Map<String, Object>> frontierData = new ArrayList<>();
        for (var r : frontier) {
            frontierData.add(Map.of(
                    "return", r.expectedReturn(), "volatility", r.volatility(),
                    "sharpe", r.sharpeRatio(), "weights", buildWeightMap(fundCodes, r.weights())
            ));
        }
        response.put("frontier", frontierData);

        // 更新目标权重
        for (int i = 0; i < Math.min(holdings.size(), fundCodes.size()); i++) {
            final String targetCode = fundCodes.get(i);
            final double targetW = finalWeights[i];
            for (PortfolioHolding h : holdings) {
                String hCode = fundRepo.findById(h.getFundId()).map(Fund::getFundCode).orElse("");
                if (targetCode.equals(hCode) && targetW > 0) {
                    h.setTargetWeight(BigDecimal.valueOf(targetW));
                    holdingRepo.save(h);
                    break;
                }
            }
        }

        return response;
    }

    /**
     * 生成再平衡方案
     */
    public RebalanceEngine.RebalancePlan rebalance(Long portfolioId) {
        Portfolio portfolio = portfolioRepo.findById(portfolioId).orElseThrow();
        List<PortfolioHolding> holdings = holdingRepo.findByPortfolioIdAndIsActiveTrue(portfolioId);

        Map<String, Double> targetWeights = new LinkedHashMap<>();
        Map<String, Double> currentWeights = new LinkedHashMap<>();

        for (PortfolioHolding h : holdings) {
            Fund fund = fundRepo.findById(h.getFundId()).orElse(null);
            if (fund == null) continue;

            targetWeights.put(fund.getFundCode(),
                    h.getTargetWeight() != null ? h.getTargetWeight().doubleValue() : 0);
            currentWeights.put(fund.getFundCode(),
                    h.getCurrentWeight() != null ? h.getCurrentWeight().doubleValue() : 0);
        }

        double currentValue = portfolio.getCurrentValue() != null
                ? portfolio.getCurrentValue().doubleValue() : 0;

        return rebalancer.generatePlan(targetWeights, currentWeights, currentValue, 0.05);
    }

    public List<Portfolio> listPortfolios() {
        return portfolioRepo.findByIsActiveTrue();
    }

    public Optional<Portfolio> getPortfolio(Long id) {
        return portfolioRepo.findById(id);
    }

    public List<PortfolioHolding> getHoldings(Long portfolioId) {
        return holdingRepo.findByPortfolioIdAndIsActiveTrue(portfolioId);
    }

    // ========== 工具 ==========

    private double[][] loadReturnMatrix(List<PortfolioHolding> holdings,
                                         LocalDate start, LocalDate end,
                                         List<String> fundCodesOut) {
        List<double[]> allReturns = new ArrayList<>();
        int minLen = Integer.MAX_VALUE;

        for (PortfolioHolding h : holdings) {
            Fund fund = fundRepo.findById(h.getFundId()).orElse(null);
            if (fund == null) continue;

            List<BigDecimal> dailyReturns = navRepo.findDailyReturnSeries(
                    fund.getId(), start, end);
            if (dailyReturns.size() < 60) continue;

            double[] retArray = dailyReturns.stream()
                    .mapToDouble(BigDecimal::doubleValue).toArray();
            allReturns.add(retArray);
            fundCodesOut.add(fund.getFundCode());
            minLen = Math.min(minLen, retArray.length);
        }

        if (allReturns.isEmpty()) return null;

        // 对齐长度，转置为 returns[day][asset]
        int assets = allReturns.size();
        double[][] returns = new double[minLen][assets];
        for (int j = 0; j < assets; j++) {
            double[] src = allReturns.get(j);
            int offset = src.length - minLen;
            for (int i = 0; i < minLen; i++) {
                returns[i][j] = src[offset + i];
            }
        }
        return returns;
    }

    private Map<String, Double> buildWeightMap(List<String> codes, double[] weights) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(codes.size(), weights.length); i++) {
            if (weights[i] > 0.001) {
                map.put(codes.get(i), Math.round(weights[i] * 10000.0) / 100.0);
            }
        }
        return map;
    }
}
