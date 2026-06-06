package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "backtest_results")
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backtest_id", nullable = false, unique = true)
    private Long backtestId;

    @Column(name = "total_return", precision = 12, scale = 6)
    private BigDecimal totalReturn;

    @Column(name = "annual_return", precision = 12, scale = 6)
    private BigDecimal annualReturn;

    @Column(precision = 12, scale = 6)
    private BigDecimal volatility;

    @Column(name = "sharpe_ratio", precision = 12, scale = 6)
    private BigDecimal sharpeRatio;

    @Column(name = "sortino_ratio", precision = 12, scale = 6)
    private BigDecimal sortinoRatio;

    @Column(name = "calmar_ratio", precision = 12, scale = 6)
    private BigDecimal calmarRatio;

    @Column(name = "max_drawdown", precision = 12, scale = 6)
    private BigDecimal maxDrawdown;

    @Column(name = "max_drawdown_start")
    private LocalDate maxDrawdownStart;

    @Column(name = "max_drawdown_end")
    private LocalDate maxDrawdownEnd;

    @Column(name = "win_rate", precision = 8, scale = 4)
    private BigDecimal winRate;

    @Column(name = "total_trades")
    private Integer totalTrades;

    @Column(name = "benchmark_return", precision = 12, scale = 6)
    private BigDecimal benchmarkReturn;

    @Column(name = "excess_return", precision = 12, scale = 6)
    private BigDecimal excessReturn;

    @Column(precision = 12, scale = 6)
    private BigDecimal alpha;

    @Column(precision = 12, scale = 6)
    private BigDecimal beta;

    @Column(name = "monthly_returns", columnDefinition = "JSON")
    private String monthlyReturns;

    @Column(name = "yearly_returns", columnDefinition = "JSON")
    private String yearlyReturns;

    @Column(name = "equity_curve", columnDefinition = "JSON")
    private String equityCurve;

    @Column(name = "trade_log", columnDefinition = "JSON")
    private String tradeLog;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBacktestId() { return backtestId; }
    public void setBacktestId(Long id) { this.backtestId = id; }
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
    public Integer getTotalTrades() { return totalTrades; }
    public void setTotalTrades(Integer t) { this.totalTrades = t; }
    public BigDecimal getBenchmarkReturn() { return benchmarkReturn; }
    public void setBenchmarkReturn(BigDecimal r) { this.benchmarkReturn = r; }
    public BigDecimal getExcessReturn() { return excessReturn; }
    public void setExcessReturn(BigDecimal r) { this.excessReturn = r; }
    public BigDecimal getAlpha() { return alpha; }
    public void setAlpha(BigDecimal a) { this.alpha = a; }
    public BigDecimal getBeta() { return beta; }
    public void setBeta(BigDecimal b) { this.beta = b; }
    public String getMonthlyReturns() { return monthlyReturns; }
    public void setMonthlyReturns(String s) { this.monthlyReturns = s; }
    public String getYearlyReturns() { return yearlyReturns; }
    public void setYearlyReturns(String s) { this.yearlyReturns = s; }
    public String getEquityCurve() { return equityCurve; }
    public void setEquityCurve(String c) { this.equityCurve = c; }
    public String getTradeLog() { return tradeLog; }
    public void setTradeLog(String t) { this.tradeLog = t; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
