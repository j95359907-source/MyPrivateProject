package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金净值历史 —— 核心时间序列实体
 */
@Entity
@Table(name = "nav_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"fund_id", "nav_date"}),
       indexes = {
           @Index(name = "idx_nav_fund_date", columnList = "fund_id, nav_date DESC"),
           @Index(name = "idx_nav_date", columnList = "nav_date")
       })
public class NavHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_id", nullable = false)
    private Long fundId;

    @Column(name = "nav_date", nullable = false)
    private LocalDate navDate;

    @Column(name = "unit_nav", precision = 12, scale = 4)
    private BigDecimal unitNav;

    @Column(name = "acc_nav", precision = 12, scale = 4)
    private BigDecimal accNav;

    @Column(name = "daily_return", precision = 10, scale = 6)
    private BigDecimal dailyReturn;

    @Column(name = "adj_nav", precision = 12, scale = 4)
    private BigDecimal adjNav;

    // ETF特有
    @Column(name = "iopv", precision = 12, scale = 4)
    private BigDecimal iopv;

    @Column(name = "premium_rate", precision = 8, scale = 4)
    private BigDecimal premiumRate;

    @Column(name = "etf_volume")
    private Long etfVolume;

    @Column(name = "etf_amount", precision = 20, scale = 2)
    private BigDecimal etfAmount;

    // 分红拆分
    @Column(name = "dividend", precision = 12, scale = 4)
    private BigDecimal dividend;

    @Column(name = "split_ratio", precision = 8, scale = 6)
    private BigDecimal splitRatio;

    @Column(name = "data_source", length = 20)
    private String dataSource;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // -- Getters / Setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }

    public LocalDate getNavDate() { return navDate; }
    public void setNavDate(LocalDate navDate) { this.navDate = navDate; }

    public BigDecimal getUnitNav() { return unitNav; }
    public void setUnitNav(BigDecimal unitNav) { this.unitNav = unitNav; }

    public BigDecimal getAccNav() { return accNav; }
    public void setAccNav(BigDecimal accNav) { this.accNav = accNav; }

    public BigDecimal getDailyReturn() { return dailyReturn; }
    public void setDailyReturn(BigDecimal dailyReturn) { this.dailyReturn = dailyReturn; }

    public BigDecimal getAdjNav() { return adjNav; }
    public void setAdjNav(BigDecimal adjNav) { this.adjNav = adjNav; }

    public BigDecimal getIopv() { return iopv; }
    public void setIopv(BigDecimal iopv) { this.iopv = iopv; }

    public BigDecimal getPremiumRate() { return premiumRate; }
    public void setPremiumRate(BigDecimal premiumRate) { this.premiumRate = premiumRate; }

    public Long getEtfVolume() { return etfVolume; }
    public void setEtfVolume(Long etfVolume) { this.etfVolume = etfVolume; }

    public BigDecimal getEtfAmount() { return etfAmount; }
    public void setEtfAmount(BigDecimal etfAmount) { this.etfAmount = etfAmount; }

    public BigDecimal getDividend() { return dividend; }
    public void setDividend(BigDecimal dividend) { this.dividend = dividend; }

    public BigDecimal getSplitRatio() { return splitRatio; }
    public void setSplitRatio(BigDecimal splitRatio) { this.splitRatio = splitRatio; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
