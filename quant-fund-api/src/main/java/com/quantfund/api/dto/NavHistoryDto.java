package com.quantfund.api.dto;

import com.quantfund.common.entity.NavHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NavHistoryDto(
        LocalDate navDate,
        BigDecimal unitNav,
        BigDecimal accNav,
        BigDecimal dailyReturn,
        BigDecimal adjNav,
        BigDecimal premiumRate,
        Long etfVolume
) {
    public static NavHistoryDto fromEntity(NavHistory n) {
        return new NavHistoryDto(
                n.getNavDate(), n.getUnitNav(), n.getAccNav(),
                n.getDailyReturn(), n.getAdjNav(),
                n.getPremiumRate(), n.getEtfVolume()
        );
    }
}
