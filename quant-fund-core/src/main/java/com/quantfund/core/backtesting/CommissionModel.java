package com.quantfund.core.backtesting;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 中国基金费率模型
 *
 * 关键认知：管理费和托管费已包含在公布的NAV中，
 * 回测仅需额外计算申购费和赎回费。
 */
public class CommissionModel {

    // 默认费率
    private BigDecimal subscriptionRate = new BigDecimal("0.0015"); // 0.15% 申购费（折后）
    private BigDecimal redemptionRate = new BigDecimal("0.0050");   // 0.50% 赎回费（持有<1年）
    private BigDecimal etfCommissionRate = new BigDecimal("0.0003"); // 万分之3 ETF佣金

    private boolean isEtf = false;

    public CommissionModel() {}

    /** ETF模式 */
    public static CommissionModel etf() {
        CommissionModel cm = new CommissionModel();
        cm.isEtf = true;
        return cm;
    }

    /** 公募基金A类模式 */
    public static CommissionModel publicFund() {
        return new CommissionModel();
    }

    /** 公募基金C类模式（0申购费） */
    public static CommissionModel publicFundC() {
        CommissionModel cm = new CommissionModel();
        cm.subscriptionRate = BigDecimal.ZERO;
        return cm;
    }

    /**
     * 计算申购费（前端）
     */
    public BigDecimal subscriptionFee(BigDecimal amount) {
        if (isEtf) {
            // ETF按佣金率计算
            return amount.multiply(etfCommissionRate)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(subscriptionRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算赎回费
     */
    public BigDecimal redemptionFee(BigDecimal amount) {
        if (isEtf) {
            return amount.multiply(etfCommissionRate)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(redemptionRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // 自定义费率
    public void setSubscriptionRate(BigDecimal rate) { this.subscriptionRate = rate; }
    public void setRedemptionRate(BigDecimal rate) { this.redemptionRate = rate; }
    public void setEtfCommissionRate(BigDecimal rate) { this.etfCommissionRate = rate; }

    public BigDecimal getSubscriptionRate() { return subscriptionRate; }
    public BigDecimal getRedemptionRate() { return redemptionRate; }
    public BigDecimal getEtfCommissionRate() { return etfCommissionRate; }
    public boolean isEtf() { return isEtf; }
}
