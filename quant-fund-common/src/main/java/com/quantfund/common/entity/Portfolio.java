package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "portfolio_type", length = 20)
    private String portfolioType = "CUSTOM";

    @Column(name = "initial_capital", precision = 16, scale = 2)
    private BigDecimal initialCapital;

    @Column(name = "current_value", precision = 16, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "cash_balance", precision = 16, scale = 2)
    private BigDecimal cashBalance = BigDecimal.ZERO;

    @Column(name = "risk_free_rate", precision = 6, scale = 4)
    private BigDecimal riskFreeRate = new BigDecimal("0.0200");

    @Column(name = "target_volatility", precision = 6, scale = 4)
    private BigDecimal targetVolatility;

    @Column(name = "rebalance_freq", length = 20)
    private String rebalanceFreq = "MONTHLY";

    @Column(name = "last_rebalanced")
    private LocalDate lastRebalanced;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "strategy_id")
    private Long strategyId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPortfolioType() { return portfolioType; }
    public void setPortfolioType(String portfolioType) { this.portfolioType = portfolioType; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public void setInitialCapital(BigDecimal initialCapital) { this.initialCapital = initialCapital; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public BigDecimal getRiskFreeRate() { return riskFreeRate; }
    public void setRiskFreeRate(BigDecimal riskFreeRate) { this.riskFreeRate = riskFreeRate; }
    public BigDecimal getTargetVolatility() { return targetVolatility; }
    public void setTargetVolatility(BigDecimal targetVolatility) { this.targetVolatility = targetVolatility; }
    public String getRebalanceFreq() { return rebalanceFreq; }
    public void setRebalanceFreq(String rebalanceFreq) { this.rebalanceFreq = rebalanceFreq; }
    public LocalDate getLastRebalanced() { return lastRebalanced; }
    public void setLastRebalanced(LocalDate lastRebalanced) { this.lastRebalanced = lastRebalanced; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public Long getStrategyId() { return strategyId; }
    public void setStrategyId(Long strategyId) { this.strategyId = strategyId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
