package com.quantfund.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金筛选指标（预计算表）
 */
@Entity
@Table(name = "screening_metrics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"fund_id", "calc_date"}),
       indexes = {
           @Index(name = "idx_metrics_date", columnList = "calc_date DESC"),
           @Index(name = "idx_sharpe_1y", columnList = "sharpe_1y"),
           @Index(name = "idx_return_1y", columnList = "return_1y")
       })
public class ScreeningMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_id", nullable = false)
    private Long fundId;

    @Column(name = "calc_date", nullable = false)
    private LocalDate calcDate;

    @Column(name = "return_1m", precision = 12, scale = 6)
    private BigDecimal return1m;

    @Column(name = "return_3m", precision = 12, scale = 6)
    private BigDecimal return3m;

    @Column(name = "return_6m", precision = 12, scale = 6)
    private BigDecimal return6m;

    @Column(name = "return_1y", precision = 12, scale = 6)
    private BigDecimal return1y;

    @Column(name = "return_3y", precision = 12, scale = 6)
    private BigDecimal return3y;

    @Column(name = "return_5y", precision = 12, scale = 6)
    private BigDecimal return5y;

    @Column(name = "return_ytd", precision = 12, scale = 6)
    private BigDecimal returnYtd;

    @Column(name = "volatility_1y", precision = 12, scale = 6)
    private BigDecimal volatility1y;

    @Column(name = "volatility_3y", precision = 12, scale = 6)
    private BigDecimal volatility3y;

    @Column(name = "max_drawdown_1y", precision = 12, scale = 6)
    private BigDecimal maxDrawdown1y;

    @Column(name = "max_drawdown_3y", precision = 12, scale = 6)
    private BigDecimal maxDrawdown3y;

    @Column(name = "downside_risk", precision = 12, scale = 6)
    private BigDecimal downsideRisk;

    @Column(name = "sharpe_1y", precision = 12, scale = 6)
    private BigDecimal sharpe1y;

    @Column(name = "sharpe_3y", precision = 12, scale = 6)
    private BigDecimal sharpe3y;

    @Column(name = "sortino_1y", precision = 12, scale = 6)
    private BigDecimal sortino1y;

    @Column(name = "sortino_3y", precision = 12, scale = 6)
    private BigDecimal sortino3y;

    @Column(name = "calmar_ratio", precision = 12, scale = 6)
    private BigDecimal calmarRatio;

    @Column(name = "information_ratio", precision = 12, scale = 6)
    private BigDecimal informationRatio;

    @Column(name = "alpha", precision = 12, scale = 6)
    private BigDecimal alpha;

    @Column(name = "beta", precision = 12, scale = 6)
    private BigDecimal beta;

    @Column(name = "tracking_error", precision = 12, scale = 6)
    private BigDecimal trackingError;

    @Column(name = "win_rate", precision = 8, scale = 4)
    private BigDecimal winRate;

    @Column(name = "rank_1y")
    private Integer rank1y;

    @Column(name = "rank_3y")
    private Integer rank3y;

    @Column(name = "percentile_1y", precision = 6, scale = 2)
    private BigDecimal percentile1y;

    @Column(name = "data_source", length = 20)
    private String dataSource;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // -- Getters / Setters (compact) --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }
    public LocalDate getCalcDate() { return calcDate; }
    public void setCalcDate(LocalDate calcDate) { this.calcDate = calcDate; }
    public BigDecimal getReturn1m() { return return1m; }
    public void setReturn1m(BigDecimal return1m) { this.return1m = return1m; }
    public BigDecimal getReturn3m() { return return3m; }
    public void setReturn3m(BigDecimal return3m) { this.return3m = return3m; }
    public BigDecimal getReturn6m() { return return6m; }
    public void setReturn6m(BigDecimal return6m) { this.return6m = return6m; }
    public BigDecimal getReturn1y() { return return1y; }
    public void setReturn1y(BigDecimal return1y) { this.return1y = return1y; }
    public BigDecimal getReturn3y() { return return3y; }
    public void setReturn3y(BigDecimal return3y) { this.return3y = return3y; }
    public BigDecimal getReturn5y() { return return5y; }
    public void setReturn5y(BigDecimal return5y) { this.return5y = return5y; }
    public BigDecimal getReturnYtd() { return returnYtd; }
    public void setReturnYtd(BigDecimal returnYtd) { this.returnYtd = returnYtd; }
    public BigDecimal getVolatility1y() { return volatility1y; }
    public void setVolatility1y(BigDecimal volatility1y) { this.volatility1y = volatility1y; }
    public BigDecimal getVolatility3y() { return volatility3y; }
    public void setVolatility3y(BigDecimal volatility3y) { this.volatility3y = volatility3y; }
    public BigDecimal getMaxDrawdown1y() { return maxDrawdown1y; }
    public void setMaxDrawdown1y(BigDecimal maxDrawdown1y) { this.maxDrawdown1y = maxDrawdown1y; }
    public BigDecimal getMaxDrawdown3y() { return maxDrawdown3y; }
    public void setMaxDrawdown3y(BigDecimal maxDrawdown3y) { this.maxDrawdown3y = maxDrawdown3y; }
    public BigDecimal getDownsideRisk() { return downsideRisk; }
    public void setDownsideRisk(BigDecimal downsideRisk) { this.downsideRisk = downsideRisk; }
    public BigDecimal getSharpe1y() { return sharpe1y; }
    public void setSharpe1y(BigDecimal sharpe1y) { this.sharpe1y = sharpe1y; }
    public BigDecimal getSharpe3y() { return sharpe3y; }
    public void setSharpe3y(BigDecimal sharpe3y) { this.sharpe3y = sharpe3y; }
    public BigDecimal getSortino1y() { return sortino1y; }
    public void setSortino1y(BigDecimal sortino1y) { this.sortino1y = sortino1y; }
    public BigDecimal getSortino3y() { return sortino3y; }
    public void setSortino3y(BigDecimal sortino3y) { this.sortino3y = sortino3y; }
    public BigDecimal getCalmarRatio() { return calmarRatio; }
    public void setCalmarRatio(BigDecimal calmarRatio) { this.calmarRatio = calmarRatio; }
    public BigDecimal getInformationRatio() { return informationRatio; }
    public void setInformationRatio(BigDecimal informationRatio) { this.informationRatio = informationRatio; }
    public BigDecimal getAlpha() { return alpha; }
    public void setAlpha(BigDecimal alpha) { this.alpha = alpha; }
    public BigDecimal getBeta() { return beta; }
    public void setBeta(BigDecimal beta) { this.beta = beta; }
    public BigDecimal getTrackingError() { return trackingError; }
    public void setTrackingError(BigDecimal trackingError) { this.trackingError = trackingError; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    public Integer getRank1y() { return rank1y; }
    public void setRank1y(Integer rank1y) { this.rank1y = rank1y; }
    public Integer getRank3y() { return rank3y; }
    public void setRank3y(Integer rank3y) { this.rank3y = rank3y; }
    public BigDecimal getPercentile1y() { return percentile1y; }
    public void setPercentile1y(BigDecimal percentile1y) { this.percentile1y = percentile1y; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
