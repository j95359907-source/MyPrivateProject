package com.quantfund.core.backtesting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 绩效分析器 —— 计算回测结果的各项风险/收益指标
 */
public class PerformanceAnalyzer {

    private static final BigDecimal TRADING_DAYS_PER_YEAR = new BigDecimal("250");
    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.03"); // 3%

    public static void analyze(BacktestResult result) {
        List<BigDecimal> equity = result.getEquityCurveValues();
        if (equity == null || equity.size() < 2) return;

        // ---- 计算每日收益率 ----
        List<BigDecimal> dailyReturns = new ArrayList<>();
        for (int i = 1; i < equity.size(); i++) {
            BigDecimal prev = equity.get(i - 1);
            BigDecimal curr = equity.get(i);
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ret = curr.subtract(prev).divide(prev, 10, RoundingMode.HALF_UP);
                dailyReturns.add(ret);
            }
        }

        if (dailyReturns.isEmpty()) return;

        // 1. 年化收益率
        long days = ChronoUnit.DAYS.between(result.getStartDate(), result.getEndDate());
        double years = (double) days / 365.0;
        if (years > 0) {
            double totalRet = result.getTotalReturn().doubleValue();
            double annual = Math.pow(1 + totalRet, 1.0 / years) - 1;
            result.setAnnualReturn(BigDecimal.valueOf(annual));
        }

        // 2. 年化波动率 = std(dailyReturns) * sqrt(250)
        double avgRet = dailyReturns.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = dailyReturns.stream()
                .mapToDouble(r -> Math.pow(r.doubleValue() - avgRet, 2))
                .average().orElse(0);
        double dailyStd = Math.sqrt(variance);
        result.setVolatility(BigDecimal.valueOf(dailyStd * Math.sqrt(250)));

        // 3. 夏普比率 = (年化收益 - 无风险利率) / 年化波动率
        if (result.getVolatility().compareTo(BigDecimal.ZERO) > 0) {
            double sharpe = (result.getAnnualReturn().doubleValue() - RISK_FREE_RATE.doubleValue())
                    / result.getVolatility().doubleValue();
            result.setSharpeRatio(BigDecimal.valueOf(sharpe));
        }

        // 4. 最大回撤
        calculateMaxDrawdown(equity, result);

        // 5. Calmar = 年化收益 / 最大回撤
        if (result.getMaxDrawdown() != null && result.getMaxDrawdown().compareTo(BigDecimal.ZERO) != 0) {
            result.setCalmarRatio(result.getAnnualReturn().divide(
                    result.getMaxDrawdown().abs(), 4, RoundingMode.HALF_UP));
        }

        // 6. Sortino = (年化 - 无风险) / 下行标准差
        double downsideVar = dailyReturns.stream()
                .filter(r -> r.doubleValue() < 0)
                .mapToDouble(r -> Math.pow(r.doubleValue(), 2))
                .average().orElse(0);
        double downsideStd = Math.sqrt(downsideVar) * Math.sqrt(250);
        if (downsideStd > 0) {
            result.setSortinoRatio(BigDecimal.valueOf(
                    (result.getAnnualReturn().doubleValue() - RISK_FREE_RATE.doubleValue()) / downsideStd));
        }

        // 7. 胜率
        long wins = dailyReturns.stream().filter(r -> r.doubleValue() > 0).count();
        result.setWinRate(BigDecimal.valueOf((double) wins / dailyReturns.size()));

        // 8. 交易次数
        result.setTotalTrades(result.getTrades() != null ? result.getTrades().size() : 0);
    }

    private static void calculateMaxDrawdown(List<BigDecimal> equity, BacktestResult result) {
        BigDecimal peak = equity.getFirst();
        BigDecimal maxDD = BigDecimal.ZERO;
        int ddStart = 0, ddEnd = 0;
        int currentDDStart = 0;

        for (int i = 1; i < equity.size(); i++) {
            BigDecimal value = equity.get(i);
            if (value.compareTo(peak) > 0) {
                peak = value;
                currentDDStart = i;
            }
            BigDecimal dd = value.subtract(peak).divide(peak, 8, RoundingMode.HALF_UP);
            if (dd.compareTo(maxDD) < 0) {
                maxDD = dd;
                ddStart = currentDDStart;
                ddEnd = i;
            }
        }

        result.setMaxDrawdown(maxDD);
        if (ddStart < result.getEquityCurveDates().size() && ddEnd < result.getEquityCurveDates().size()) {
            result.setMaxDrawdownStart(result.getEquityCurveDates().get(ddStart));
            result.setMaxDrawdownEnd(result.getEquityCurveDates().get(ddEnd));
        }
    }
}
