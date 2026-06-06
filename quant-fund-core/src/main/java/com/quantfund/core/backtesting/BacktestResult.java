package com.quantfund.core.backtesting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 回测结果
 */
public class BacktestResult {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal initialCapital;
    private BigDecimal finalValue;
    private BigDecimal totalReturn;

    // 绩效指标（由PerformanceAnalyzer填充）
    private BigDecimal annualReturn;
    private BigDecimal volatility;
    private BigDecimal sharpeRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal calmarRatio;
    private BigDecimal maxDrawdown;
    private LocalDate maxDrawdownStart;
    private LocalDate maxDrawdownEnd;
    private BigDecimal winRate;
    private int totalTrades;

    // 详细数据
    private List<LocalDate> equityCurveDates;
    private List<BigDecimal> equityCurveValues;
    private List<BacktestEngine.TradeRecord> trades;

    // Getters / Setters
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate d) { this.startDate = d; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate d) { this.endDate = d; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public void setInitialCapital(BigDecimal c) { this.initialCapital = c; }
    public BigDecimal getFinalValue() { return finalValue; }
    public void setFinalValue(BigDecimal v) { this.finalValue = v; }
    public BigDecimal getTotalReturn() { return totalReturn; }
    public void setTotalReturn(BigDecimal r) { this.totalReturn = r; }
    public BigDecimal getAnnualReturn() { return annualReturn; }
    public void setAnnualReturn(BigDecimal r) { this.annualReturn = r; }
    public BigDecimal getVolatility() { return volatility; }
    public void setVolatility(BigDecimal v) { this.volatility = v; }
    public BigDecimal getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(BigDecimal s) { this.sharpeRatio = s; }
    public BigDecimal getSortinoRatio() { return sortinoRatio; }
    public void setSortinoRatio(BigDecimal s) { this.sortinoRatio = s; }
    public BigDecimal getCalmarRatio() { return calmarRatio; }
    public void setCalmarRatio(BigDecimal c) { this.calmarRatio = c; }
    public BigDecimal getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(BigDecimal m) { this.maxDrawdown = m; }
    public LocalDate getMaxDrawdownStart() { return maxDrawdownStart; }
    public void setMaxDrawdownStart(LocalDate d) { this.maxDrawdownStart = d; }
    public LocalDate getMaxDrawdownEnd() { return maxDrawdownEnd; }
    public void setMaxDrawdownEnd(LocalDate d) { this.maxDrawdownEnd = d; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal w) { this.winRate = w; }
    public int getTotalTrades() { return totalTrades; }
    public void setTotalTrades(int t) { this.totalTrades = t; }
    public List<LocalDate> getEquityCurveDates() { return equityCurveDates; }
    public void setEquityCurveDates(List<LocalDate> d) { this.equityCurveDates = d; }
    public List<BigDecimal> getEquityCurveValues() { return equityCurveValues; }
    public void setEquityCurveValues(List<BigDecimal> v) { this.equityCurveValues = v; }
    public List<BacktestEngine.TradeRecord> getTrades() { return trades; }
    public void setTrades(List<BacktestEngine.TradeRecord> t) { this.trades = t; }
}
