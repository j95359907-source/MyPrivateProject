package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "fund_id", nullable = false)
    private Long fundId;

    @Column(name = "trade_type", nullable = false, length = 10)
    private String tradeType;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "trade_time")
    private LocalDateTime tradeTime;

    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal shares;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(precision = 12, scale = 4)
    private BigDecimal commission = BigDecimal.ZERO;

    @Column(length = 20)
    private String status = "EXECUTED";

    @Column(name = "execution_id", length = 100)
    private String executionId;

    @Column(name = "signal_source", length = 50)
    private String signalSource;

    @Column(name = "strategy_id")
    private Long strategyId;

    @Column(name = "signal_price", precision = 12, scale = 4)
    private BigDecimal signalPrice;

    @Column(name = "signal_date")
    private LocalDateTime signalDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }
    public String getTradeType() { return tradeType; }
    public void setTradeType(String tradeType) { this.tradeType = tradeType; }
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    public LocalDateTime getTradeTime() { return tradeTime; }
    public void setTradeTime(LocalDateTime tradeTime) { this.tradeTime = tradeTime; }
    public BigDecimal getShares() { return shares; }
    public void setShares(BigDecimal shares) { this.shares = shares; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    public String getSignalSource() { return signalSource; }
    public void setSignalSource(String signalSource) { this.signalSource = signalSource; }
    public Long getStrategyId() { return strategyId; }
    public void setStrategyId(Long strategyId) { this.strategyId = strategyId; }
    public BigDecimal getSignalPrice() { return signalPrice; }
    public void setSignalPrice(BigDecimal signalPrice) { this.signalPrice = signalPrice; }
    public LocalDateTime getSignalDate() { return signalDate; }
    public void setSignalDate(LocalDateTime signalDate) { this.signalDate = signalDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
