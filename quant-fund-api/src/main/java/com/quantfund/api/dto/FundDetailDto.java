package com.quantfund.api.dto;

import com.quantfund.common.entity.Fund;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FundDetailDto(
        String fundCode, String fundName, String fundShortName,
        String fundType, String fundSubtype, String shareClass,
        Boolean isEtf, Boolean isIndex, String trackingIndex,
        String managementCompany, String custodianBank,
        LocalDate inceptionDate, LocalDate listingDate,
        BigDecimal fundSize, BigDecimal managementFee,
        BigDecimal custodyFee, BigDecimal subscriptionFee, BigDecimal redemptionFee,
        String investmentTarget, String benchmark, String sectorFocus,
        Boolean isActive
) {
    public static FundDetailDto fromEntity(Fund f) {
        return new FundDetailDto(
                f.getFundCode(), f.getFundName(), f.getFundShortName(),
                f.getFundType(), f.getFundSubtype(), f.getShareClass(),
                f.getIsEtf(), f.getIsIndex(), f.getTrackingIndex(),
                f.getManagementCompany(), f.getCustodianBank(),
                f.getInceptionDate(), f.getListingDate(),
                f.getFundSize(), f.getManagementFee(),
                f.getCustodyFee(), f.getSubscriptionFee(), f.getRedemptionFee(),
                f.getInvestmentTarget(), f.getBenchmark(), f.getSectorFocus(),
                f.getIsActive()
        );
    }
}
