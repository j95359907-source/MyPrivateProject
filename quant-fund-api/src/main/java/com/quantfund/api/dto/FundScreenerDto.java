package com.quantfund.api.dto;

import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.ScreeningMetrics;

import java.math.BigDecimal;

public record FundScreenerDto(
        String fundCode, String fundName, String fundShortName,
        String fundType, String managementCompany,
        BigDecimal return1m, BigDecimal return3m, BigDecimal return1y, BigDecimal return3y,
        BigDecimal volatility1y, BigDecimal maxDrawdown1y,
        BigDecimal sharpe1y, BigDecimal sortino1y, BigDecimal calmarRatio,
        BigDecimal alpha, BigDecimal beta,
        Integer rank1y, BigDecimal percentile1y
) {
    public static FundScreenerDto fromMetricsAndFund(ScreeningMetrics m, Fund f) {
        return new FundScreenerDto(
                f.getFundCode(), f.getFundName(), f.getFundShortName(),
                f.getFundType(), f.getManagementCompany(),
                m.getReturn1m(), m.getReturn3m(), m.getReturn1y(), m.getReturn3y(),
                m.getVolatility1y(), m.getMaxDrawdown1y(),
                m.getSharpe1y(), m.getSortino1y(), m.getCalmarRatio(),
                m.getAlpha(), m.getBeta(),
                m.getRank1y(), m.getPercentile1y()
        );
    }
}
