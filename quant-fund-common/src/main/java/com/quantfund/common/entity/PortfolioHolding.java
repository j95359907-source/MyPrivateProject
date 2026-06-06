package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_holdings")
public class PortfolioHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "fund_id", nullable = false)
    private Long fundId;

    @Column(name = "target_weight", nullable = false, precision = 8, scale = 4)
    private BigDecimal targetWeight;

    @Column(name = "current_weight", precision = 8, scale = 4)
    private BigDecimal currentWeight;

    @Column(name = "shares_held", precision = 16, scale = 4)
    private BigDecimal sharesHeld;

    @Column(name = "avg_cost", precision = 12, scale = 4)
    private BigDecimal avgCost;

    @Column(name = "current_value", precision = 16, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "unrealized_pnl", precision = 12, scale = 2)
    private BigDecimal unrealizedPnl;

    @Column(name = "total_return", precision = 12, scale = 6)
    private BigDecimal totalReturn;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long id) { this.portfolioId = id; }
    public Long getFundId() { return fundId; }
    public void setFundId(Long id) { this.fundId = id; }
    public BigDecimal getTargetWeight() { return targetWeight; }
    public void setTargetWeight(BigDecimal w) { this.targetWeight = w; }
    public BigDecimal getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(BigDecimal w) { this.currentWeight = w; }
    public BigDecimal getSharesHeld() { return sharesHeld; }
    public void setSharesHeld(BigDecimal s) { this.sharesHeld = s; }
    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal c) { this.avgCost = c; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal v) { this.currentValue = v; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal p) { this.unrealizedPnl = p; }
    public BigDecimal getTotalReturn() { return totalReturn; }
    public void setTotalReturn(BigDecimal r) { this.totalReturn = r; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean a) { this.isActive = a; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate d) { this.entryDate = d; }
    public LocalDate getExitDate() { return exitDate; }
    public void setExitDate(LocalDate d) { this.exitDate = d; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
