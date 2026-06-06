package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 策略定义实体
 */
@Entity
@Table(name = "strategies")
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "strategy_type", nullable = false, length = 30)
    private String strategyType;

    @Column(columnDefinition = "JSON", nullable = false)
    private String parameters;

    @Column(name = "target_universe", length = 20)
    private String targetUniverse;

    @Column(name = "rebalance_freq", length = 20)
    private String rebalanceFreq;

    @Column(name = "max_holdings")
    private Integer maxHoldings = 10;

    private Integer version = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

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

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    public String getTargetUniverse() { return targetUniverse; }
    public void setTargetUniverse(String targetUniverse) { this.targetUniverse = targetUniverse; }
    public String getRebalanceFreq() { return rebalanceFreq; }
    public void setRebalanceFreq(String rebalanceFreq) { this.rebalanceFreq = rebalanceFreq; }
    public Integer getMaxHoldings() { return maxHoldings; }
    public void setMaxHoldings(Integer maxHoldings) { this.maxHoldings = maxHoldings; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
