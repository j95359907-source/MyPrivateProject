package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金主表实体
 */
@Entity
@Table(name = "funds")
public class Fund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_code", nullable = false, unique = true, length = 10)
    private String fundCode;

    @Column(name = "fund_name", nullable = false, length = 200)
    private String fundName;

    @Column(name = "fund_short_name", length = 50)
    private String fundShortName;

    @Column(name = "fund_type", nullable = false, length = 30)
    private String fundType;

    @Column(name = "fund_subtype", length = 30)
    private String fundSubtype;

    @Column(name = "share_class", length = 5)
    private String shareClass;

    @Column(name = "is_etf")
    private Boolean isEtf = false;

    @Column(name = "is_index")
    private Boolean isIndex = false;

    @Column(name = "tracking_index", length = 100)
    private String trackingIndex;

    @Column(name = "management_company", length = 200)
    private String managementCompany;

    @Column(name = "custodian_bank", length = 100)
    private String custodianBank;

    @Column(name = "inception_date")
    private LocalDate inceptionDate;

    @Column(name = "listing_date")
    private LocalDate listingDate;

    @Column(name = "fund_size", precision = 20, scale = 2)
    private BigDecimal fundSize;

    @Column(name = "fund_size_date")
    private LocalDate fundSizeDate;

    @Column(name = "management_fee", precision = 6, scale = 4)
    private BigDecimal managementFee;

    @Column(name = "custody_fee", precision = 6, scale = 4)
    private BigDecimal custodyFee;

    @Column(name = "subscription_fee", precision = 6, scale = 4)
    private BigDecimal subscriptionFee;

    @Column(name = "redemption_fee", precision = 6, scale = 4)
    private BigDecimal redemptionFee;

    @Column(name = "investment_target", columnDefinition = "TEXT")
    private String investmentTarget;

    @Column(name = "benchmark", length = 500)
    private String benchmark;

    @Column(name = "sector_focus", length = 100)
    private String sectorFocus;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "data_source", length = 20)
    private String dataSource;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // -- Getters / Setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public String getFundShortName() { return fundShortName; }
    public void setFundShortName(String fundShortName) { this.fundShortName = fundShortName; }

    public String getFundType() { return fundType; }
    public void setFundType(String fundType) { this.fundType = fundType; }

    public String getFundSubtype() { return fundSubtype; }
    public void setFundSubtype(String fundSubtype) { this.fundSubtype = fundSubtype; }

    public String getShareClass() { return shareClass; }
    public void setShareClass(String shareClass) { this.shareClass = shareClass; }

    public Boolean getIsEtf() { return isEtf; }
    public void setIsEtf(Boolean isEtf) { this.isEtf = isEtf; }

    public Boolean getIsIndex() { return isIndex; }
    public void setIsIndex(Boolean isIndex) { this.isIndex = isIndex; }

    public String getTrackingIndex() { return trackingIndex; }
    public void setTrackingIndex(String trackingIndex) { this.trackingIndex = trackingIndex; }

    public String getManagementCompany() { return managementCompany; }
    public void setManagementCompany(String managementCompany) { this.managementCompany = managementCompany; }

    public String getCustodianBank() { return custodianBank; }
    public void setCustodianBank(String custodianBank) { this.custodianBank = custodianBank; }

    public LocalDate getInceptionDate() { return inceptionDate; }
    public void setInceptionDate(LocalDate inceptionDate) { this.inceptionDate = inceptionDate; }

    public LocalDate getListingDate() { return listingDate; }
    public void setListingDate(LocalDate listingDate) { this.listingDate = listingDate; }

    public BigDecimal getFundSize() { return fundSize; }
    public void setFundSize(BigDecimal fundSize) { this.fundSize = fundSize; }

    public LocalDate getFundSizeDate() { return fundSizeDate; }
    public void setFundSizeDate(LocalDate fundSizeDate) { this.fundSizeDate = fundSizeDate; }

    public BigDecimal getManagementFee() { return managementFee; }
    public void setManagementFee(BigDecimal managementFee) { this.managementFee = managementFee; }

    public BigDecimal getCustodyFee() { return custodyFee; }
    public void setCustodyFee(BigDecimal custodyFee) { this.custodyFee = custodyFee; }

    public BigDecimal getSubscriptionFee() { return subscriptionFee; }
    public void setSubscriptionFee(BigDecimal subscriptionFee) { this.subscriptionFee = subscriptionFee; }

    public BigDecimal getRedemptionFee() { return redemptionFee; }
    public void setRedemptionFee(BigDecimal redemptionFee) { this.redemptionFee = redemptionFee; }

    public String getInvestmentTarget() { return investmentTarget; }
    public void setInvestmentTarget(String investmentTarget) { this.investmentTarget = investmentTarget; }

    public String getBenchmark() { return benchmark; }
    public void setBenchmark(String benchmark) { this.benchmark = benchmark; }

    public String getSectorFocus() { return sectorFocus; }
    public void setSectorFocus(String sectorFocus) { this.sectorFocus = sectorFocus; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
