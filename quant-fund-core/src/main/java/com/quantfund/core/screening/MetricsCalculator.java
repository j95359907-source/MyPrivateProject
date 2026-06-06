package com.quantfund.core.screening;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 量化指标计算引擎 —— 纯Java实现所有基金评估指标
 *
 * 输入：净值序列 + 日期序列 + 基准收益序列（可选）
 * 输出：MetricsResult 包含所有计算好的指标
 *
 * 关键假设：
 * - 公募基金按日频NAV计算，标准年化因子 = sqrt(250)
 * - 无风险利率默认 3%
 */
public class MetricsCalculator {

    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("250");
    private static final BigDecimal SQRT_250 = new BigDecimal("15.811388300841896");
    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.03");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * 计算单个基金的全部指标
     */
    public MetricsResult calculate(
            List<BigDecimal> navSeries,
            List<LocalDate> dates,
            List<BigDecimal> benchmarkReturns // 可为null
    ) {
        // 1. 计算日收益率序列
        List<BigDecimal> dailyReturns = calcDailyReturns(navSeries);
        if (dailyReturns.isEmpty()) return MetricsResult.empty();

        // 2. 收益指标
        MetricsResult result = new MetricsResult();
        result.return1m  = periodReturn(dailyReturns, dates, 21);   // 约1个月交易日
        result.return3m  = periodReturn(dailyReturns, dates, 63);
        result.return6m  = periodReturn(dailyReturns, dates, 126);
        result.return1y  = periodReturn(dailyReturns, dates, 250);
        result.return3y  = annualizedReturn(dailyReturns, dates, 750);
        result.return5y  = annualizedReturn(dailyReturns, dates, 1250);
        result.returnYtd = ytdReturn(dailyReturns, dates);
        result.returnSinceInception = totalReturn(navSeries);

        // 3. 风险指标
        result.volatility1y = annualizedVolatility(dailyReturns, 250);
        result.volatility3y = annualizedVolatility(dailyReturns, 750);
        result.maxDrawdown1y = maxDrawdown(dailyReturns, navSeries, dates, 250);
        result.maxDrawdown3y = maxDrawdown(dailyReturns, navSeries, dates, 750);
        result.downsideRisk = downsideDeviation(dailyReturns, 250);

        // 4. 风险调整收益
        result.sharpe1y  = sharpeRatio(result.return1y, result.volatility1y);
        result.sharpe3y  = sharpeRatio(result.return3y, result.volatility3y);
        result.sortino1y = sortinoRatio(result.return1y, dailyReturns, 250);
        result.sortino3y = sortinoRatio(result.return3y, dailyReturns, 750);
        result.calmarRatio = calmarRatio(result.return1y, result.maxDrawdown1y);

        // 5. 因子指标（需要基准数据）
        if (benchmarkReturns != null && !benchmarkReturns.isEmpty()) {
            calcFactorMetrics(result, dailyReturns, benchmarkReturns);
        }

        // 6. 其他统计
        calcStatsMetrics(result, dailyReturns, dates);

        return result;
    }

    // ============ 日收益率 ============

    private List<BigDecimal> calcDailyReturns(List<BigDecimal> navs) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < navs.size(); i++) {
            BigDecimal prev = navs.get(i - 1);
            BigDecimal curr = navs.get(i);
            if (prev.compareTo(BigDecimal.ZERO) > 0 && curr.compareTo(BigDecimal.ZERO) > 0) {
                returns.add(curr.subtract(prev).divide(prev, MC));
            }
        }
        return returns;
    }

    // ============ 收益计算 ============

    /** 区间累计收益（取最近N个交易日） */
    private BigDecimal periodReturn(List<BigDecimal> dailyReturns, List<LocalDate> dates, int days) {
        int n = Math.min(days, dailyReturns.size());
        if (n == 0) return BigDecimal.ZERO;
        return cumulativeReturn(dailyReturns.subList(Math.max(0, dailyReturns.size() - n), dailyReturns.size()));
    }

    /** 累计收益 = Π(1+ri) - 1 */
    private BigDecimal cumulativeReturn(List<BigDecimal> returns) {
        BigDecimal product = BigDecimal.ONE;
        for (BigDecimal r : returns) {
            product = product.multiply(BigDecimal.ONE.add(r));
        }
        return product.subtract(BigDecimal.ONE);
    }

    /** 年化收益 = (1+累计收益)^(250/N) - 1 */
    private BigDecimal annualizedReturn(List<BigDecimal> dailyReturns, List<LocalDate> dates, int days) {
        int n = Math.min(days, dailyReturns.size());
        if (n == 0) return BigDecimal.ZERO;
        BigDecimal cumRet = cumulativeReturn(
                dailyReturns.subList(Math.max(0, dailyReturns.size() - n), dailyReturns.size()));
        double years = (double) n / 250.0;
        if (years <= 0) return BigDecimal.ZERO;
        double annual = Math.pow(1.0 + cumRet.doubleValue(), 1.0 / years) - 1.0;
        return BigDecimal.valueOf(annual);
    }

    /** 年初至今收益 */
    private BigDecimal ytdReturn(List<BigDecimal> dailyReturns, List<LocalDate> dates) {
        if (dates.isEmpty()) return BigDecimal.ZERO;
        LocalDate firstOfYear = LocalDate.of(dates.get(dates.size() - 1).getYear(), 1, 1);
        int idx = 0;
        for (int i = dates.size() - 1; i >= 0; i--) {
            if (!dates.get(i).isBefore(firstOfYear)) idx = i;
            else break;
        }
        return cumulativeReturn(dailyReturns.subList(idx, dailyReturns.size()));
    }

    /** 总收益 = 最终净值/初始净值 - 1 */
    private BigDecimal totalReturn(List<BigDecimal> navs) {
        if (navs.size() < 2) return BigDecimal.ZERO;
        BigDecimal first = navs.get(0);
        BigDecimal last = navs.get(navs.size() - 1);
        if (first.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return last.subtract(first).divide(first, MC);
    }

    // ============ 风险计算 ============

    /** 年化波动率 = std(日收益) * sqrt(250) */
    private BigDecimal annualizedVolatility(List<BigDecimal> dailyReturns, int lookback) {
        int n = Math.min(lookback, dailyReturns.size());
        if (n < 2) return BigDecimal.ZERO;
        List<BigDecimal> window = dailyReturns.subList(Math.max(0, dailyReturns.size() - n), dailyReturns.size());

        double mean = window.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = window.stream()
                .mapToDouble(r -> Math.pow(r.doubleValue() - mean, 2))
                .average().orElse(0);
        return BigDecimal.valueOf(Math.sqrt(variance) * SQRT_250.doubleValue());
    }

    /** 最大回撤 */
    public BigDecimal maxDrawdown(List<BigDecimal> dailyReturns, List<BigDecimal> navs,
                                   List<LocalDate> dates, int lookback) {
        int n = Math.min(lookback, navs.size());
        if (n < 2) return BigDecimal.ZERO;

        int start = Math.max(0, navs.size() - n - 1); // +1因为navs比returns多1个
        BigDecimal peak = navs.get(start);
        BigDecimal maxDD = BigDecimal.ZERO;

        for (int i = start + 1; i < navs.size(); i++) {
            BigDecimal value = navs.get(i);
            if (value.compareTo(peak) > 0) {
                peak = value;
            }
            BigDecimal dd = value.subtract(peak).divide(peak, MC);
            if (dd.compareTo(maxDD) < 0) {
                maxDD = dd;
            }
        }
        return maxDD;
    }

    /** 下行标准差 */
    private BigDecimal downsideDeviation(List<BigDecimal> dailyReturns, int lookback) {
        int n = Math.min(lookback, dailyReturns.size());
        if (n < 2) return BigDecimal.ZERO;
        List<BigDecimal> window = dailyReturns.subList(Math.max(0, dailyReturns.size() - n), dailyReturns.size());

        double sumSq = window.stream()
                .filter(r -> r.doubleValue() < 0)
                .mapToDouble(r -> Math.pow(r.doubleValue(), 2))
                .sum();
        return BigDecimal.valueOf(Math.sqrt(sumSq / n) * SQRT_250.doubleValue());
    }

    // ============ 风险调整收益 ============

    /** 夏普比率 = (年化收益 - 无风险利率) / 年化波动率 */
    private BigDecimal sharpeRatio(BigDecimal annualReturn, BigDecimal volatility) {
        if (volatility.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return annualReturn.subtract(RISK_FREE_RATE).divide(volatility, MC);
    }

    /** Sortino比率 = (年化收益 - 无风险利率) / 下行标准差 */
    private BigDecimal sortinoRatio(BigDecimal annualReturn, List<BigDecimal> dailyReturns, int lookback) {
        BigDecimal downside = downsideDeviation(dailyReturns, lookback);
        if (downside.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return annualReturn.subtract(RISK_FREE_RATE).divide(downside, MC);
    }

    /** Calmar比率 = 年化收益 / |最大回撤| */
    private BigDecimal calmarRatio(BigDecimal annualReturn, BigDecimal maxDrawdown) {
        if (maxDrawdown.compareTo(BigDecimal.ZERO) >= 0 || maxDrawdown.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        return annualReturn.divide(maxDrawdown.abs(), MC);
    }

    // ============ 因子指标 ============

    private void calcFactorMetrics(MetricsResult r, List<BigDecimal> fundReturns, List<BigDecimal> benchReturns) {
        int n = Math.min(fundReturns.size(), benchReturns.size());
        if (n < 2) return;

        List<BigDecimal> fr = fundReturns.subList(Math.max(0, fundReturns.size() - n), fundReturns.size());
        List<BigDecimal> br = benchReturns.subList(Math.max(0, benchReturns.size() - n), benchReturns.size());

        double[] x = br.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        double[] y = fr.stream().mapToDouble(BigDecimal::doubleValue).toArray();

        // 简单线性回归: y = α + β*x
        double meanX = Arrays.stream(x).average().orElse(0);
        double meanY = Arrays.stream(y).average().orElse(0);

        double cov = 0, varX = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX;
            cov += dx * (y[i] - meanY);
            varX += dx * dx;
        }

        if (varX > 0) {
            double beta = cov / varX;
            double alpha = meanY - beta * meanX;

            r.beta = BigDecimal.valueOf(beta);
            r.alpha = BigDecimal.valueOf(alpha * 250); // 年化Alpha

            // 跟踪误差 = std(基金收益 - 基准收益)
            double te = Math.sqrt(Arrays.stream(y).map(d -> {
                double diff = d - x[0]; // 简化：每期与基准之差
                return 0; // placeholder
            }).average().orElse(0));

            double sumSqDiff = 0;
            for (int i = 0; i < n; i++) {
                double diff = y[i] - x[i];
                sumSqDiff += diff * diff;
            }
            te = Math.sqrt(sumSqDiff / n) * SQRT_250.doubleValue();
            r.trackingError = BigDecimal.valueOf(te);

            // 信息比率 = α年化 / 跟踪误差
            if (te > 0) {
                r.informationRatio = r.alpha.divide(BigDecimal.valueOf(te), MC);
            }
        }
    }

    // ============ 统计指标 ============

    private void calcStatsMetrics(MetricsResult r, List<BigDecimal> dailyReturns, List<LocalDate> dates) {
        if (dailyReturns.isEmpty()) return;

        long wins = dailyReturns.stream().filter(d -> d.doubleValue() > 0).count();
        long losses = dailyReturns.stream().filter(d -> d.doubleValue() < 0).count();

        r.winRate = BigDecimal.valueOf((double) wins / (wins + losses));

        double avgWin = dailyReturns.stream()
                .filter(d -> d.doubleValue() > 0)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double avgLoss = dailyReturns.stream()
                .filter(d -> d.doubleValue() < 0)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0);

        r.avgWin = BigDecimal.valueOf(avgWin);
        r.avgLoss = BigDecimal.valueOf(avgLoss);
        r.profitMonths = countProfitMonths(dailyReturns, dates);
    }

    /** 月度盈利月份数 */
    private int countProfitMonths(List<BigDecimal> dailyReturns, List<LocalDate> dates) {
        Map<String, BigDecimal> monthlyReturns = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(dailyReturns.size(), dates.size()); i++) {
            String key = dates.get(i).getYear() + "-" + String.format("%02d", dates.get(i).getMonthValue());
            monthlyReturns.merge(key, dailyReturns.get(i), BigDecimal::add);
        }
        return (int) monthlyReturns.values().stream().filter(r -> r.doubleValue() > 0).count();
    }

    // ============ 结果类 ============

    public static class MetricsResult {
        // 收益
        public BigDecimal return1m, return3m, return6m, return1y, return3y, return5y, returnYtd, returnSinceInception;
        // 风险
        public BigDecimal volatility1y, volatility3y, maxDrawdown1y, maxDrawdown3y, downsideRisk;
        // 风险调整
        public BigDecimal sharpe1y, sharpe3y, sortino1y, sortino3y, calmarRatio;
        // 因子
        public BigDecimal alpha, beta, trackingError, informationRatio;
        // 统计
        public BigDecimal winRate, avgWin, avgLoss;
        public int profitMonths;

        public static MetricsResult empty() {
            return new MetricsResult();
        }
    }
}
