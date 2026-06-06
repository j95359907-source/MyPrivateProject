package com.quantfund.core.backtesting;

import com.quantfund.core.strategy.Strategy;
import com.quantfund.core.strategy.Strategy.FundData;
import com.quantfund.core.strategy.Strategy.Signal;
import com.quantfund.core.strategy.Strategy.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 回测引擎核心 —— 纯Java自研
 *
 * 数据集驱动的事件循环回测：
 * 1. 加载基金池的NAV历史数据
 * 2. 按交易日逐日循环
 * 3. 每个交易日：更新持仓市值 → 调策略生成信号 → 执行交易 → 记录净值
 * 4. 循环结束 → 输出绩效报告
 */
public class BacktestEngine {

    private static final Logger log = LoggerFactory.getLogger(BacktestEngine.class);

    private final CommissionModel commissionModel;
    private final int scale = 10; // BigDecimal精度

    public BacktestEngine(CommissionModel commissionModel) {
        this.commissionModel = commissionModel;
    }

    /**
     * 运行回测
     *
     * @param strategy     策略实例
     * @param fundDataMap  基金数据（fundCode → FundData）
     * @param startDate    回测起始日
     * @param endDate      回测结束日
     * @param initialCapital 初始资金
     * @return 回测结果
     */
    public BacktestResult run(
            Strategy strategy,
            Map<String, FundData> fundDataMap,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal initialCapital) {

        log.info("Starting backtest: strategy={}, period={} → {}, capital={}",
                strategy.getName(), startDate, endDate, initialCapital);

        // 收集所有交易日（从基金数据中合并去重）
        TreeSet<LocalDate> allDates = new TreeSet<>();
        for (FundData fd : fundDataMap.values()) {
            allDates.addAll(fd.dates());
        }
        List<LocalDate> tradingDays = allDates.stream()
                .filter(d -> !d.isBefore(startDate) && !d.isAfter(endDate))
                .toList();

        if (tradingDays.isEmpty()) {
            throw new IllegalStateException("回测区间内无交易日数据");
        }

        // 初始化回测状态
        BigDecimal cash = initialCapital;
        Map<String, BigDecimal> holdings = new HashMap<>();    // fundCode → 持有份额
        Map<String, BigDecimal> navMap = new HashMap<>();      // fundCode → 当日NAV
        List<BigDecimal> equityCurve = new ArrayList<>();      // 每日总资产
        List<TradeRecord> trades = new ArrayList<>();

        // ---- 逐日循环 ----
        for (LocalDate date : tradingDays) {
            // 1. 更新当日NAV
            for (Map.Entry<String, FundData> entry : fundDataMap.entrySet()) {
                int idx = entry.getValue().dates().indexOf(date);
                if (idx >= 0 && idx < entry.getValue().navSeries().size()) {
                    navMap.put(entry.getKey(), entry.getValue().navSeries().get(idx));
                }
            }

            // 2. 计算当日总资产（现金 + 持仓市值）
            BigDecimal holdingsValue = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> h : holdings.entrySet()) {
                BigDecimal nav = navMap.get(h.getKey());
                if (nav != null) {
                    holdingsValue = holdingsValue.add(nav.multiply(h.getValue()));
                }
            }
            BigDecimal totalValue = cash.add(holdingsValue);
            equityCurve.add(totalValue);

            // 3. 构建策略上下文
            Map<String, BigDecimal> currentWeights = new HashMap<>();
            for (Map.Entry<String, BigDecimal> h : holdings.entrySet()) {
                BigDecimal nav = navMap.get(h.getKey());
                if (nav != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal weight = nav.multiply(h.getValue())
                            .divide(totalValue, scale, RoundingMode.HALF_UP);
                    currentWeights.put(h.getKey(), weight);
                }
            }

            StrategyContext ctx = new StrategyContext(date, cash, totalValue, currentWeights);

            // 4. 调用策略生成信号
            List<Signal> signals = strategy.generateSignals(ctx, fundDataMap);

            // 5. 执行信号 → 生成交易
            for (Signal signal : signals) {
                if (signal.type() == Strategy.SignalType.HOLD) continue;

                BigDecimal price = navMap.get(signal.fundCode());
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) continue;

                if (signal.type() == Strategy.SignalType.BUY) {
                    // 计算买入金额
                    BigDecimal targetValue = totalValue.multiply(signal.targetWeight());
                    BigDecimal currentValue = BigDecimal.ZERO;
                    if (holdings.containsKey(signal.fundCode()) && currentWeights.containsKey(signal.fundCode())) {
                        currentValue = totalValue.multiply(currentWeights.get(signal.fundCode()));
                    }
                    BigDecimal buyAmount = targetValue.subtract(currentValue);
                    if (buyAmount.compareTo(BigDecimal.ZERO) <= 0) continue;
                    if (buyAmount.compareTo(cash) > 0) buyAmount = cash; // 不超过现金

                    // 扣除申购费
                    BigDecimal fee = commissionModel.subscriptionFee(buyAmount);
                    BigDecimal netAmount = buyAmount.subtract(fee);
                    BigDecimal shares = netAmount.divide(price, 4, RoundingMode.HALF_DOWN);

                    cash = cash.subtract(buyAmount);
                    holdings.merge(signal.fundCode(), shares, BigDecimal::add);

                    trades.add(new TradeRecord(date, signal.fundCode(), "BUY",
                            shares, price, buyAmount, fee, signal.reason()));

                } else if (signal.type() == Strategy.SignalType.SELL) {
                    BigDecimal currentShares = holdings.getOrDefault(signal.fundCode(), BigDecimal.ZERO);
                    if (currentShares.compareTo(BigDecimal.ZERO) <= 0) continue;

                    // 卖出全部或按权重调整
                    BigDecimal sellShares = currentShares.multiply(signal.targetWeight());
                    if (sellShares.compareTo(currentShares) > 0) sellShares = currentShares;

                    BigDecimal sellAmount = sellShares.multiply(price);
                    BigDecimal fee = commissionModel.redemptionFee(sellAmount);
                    BigDecimal netAmount = sellAmount.subtract(fee);

                    cash = cash.add(netAmount);
                    BigDecimal remaining = currentShares.subtract(sellShares);
                    if (remaining.compareTo(new BigDecimal("0.0001")) <= 0) {
                        holdings.remove(signal.fundCode());
                    } else {
                        holdings.put(signal.fundCode(), remaining);
                    }

                    trades.add(new TradeRecord(date, signal.fundCode(), "SELL",
                            sellShares, price, sellAmount, fee, signal.reason()));
                }
            }
        }

        // ---- 最终结算 ----
        LocalDate finalDate = tradingDays.getLast();
        BigDecimal finalHoldingsValue = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> h : holdings.entrySet()) {
            int idx = fundDataMap.get(h.getKey()).dates().indexOf(finalDate);
            if (idx >= 0) {
                BigDecimal finalNav = fundDataMap.get(h.getKey()).navSeries().get(idx);
                finalHoldingsValue = finalHoldingsValue.add(finalNav.multiply(h.getValue()));
            }
        }
        BigDecimal finalValue = cash.add(finalHoldingsValue);

        // 构建结果
        BacktestResult result = new BacktestResult();
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        result.setInitialCapital(initialCapital);
        result.setFinalValue(finalValue);
        result.setTotalReturn(finalValue.subtract(initialCapital)
                .divide(initialCapital, scale, RoundingMode.HALF_UP));
        result.setTrades(trades);
        result.setEquityCurveDates(tradingDays);
        result.setEquityCurveValues(equityCurve);

        // 计算绩效指标
        PerformanceAnalyzer.analyze(result);

        log.info("Backtest completed: return={}%, trades={}",
                result.getTotalReturn().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                trades.size());

        return result;
    }

    /** 交易记录 */
    public record TradeRecord(
            LocalDate date, String fundCode, String type,
            BigDecimal shares, BigDecimal price, BigDecimal amount,
            BigDecimal fee, String reason
    ) {}
}
