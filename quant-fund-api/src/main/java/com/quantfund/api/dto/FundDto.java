package com.quantfund.api.dto;

import com.quantfund.common.entity.Fund;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 基金列表项DTO
 */
public record FundDto(
        Long id,
        String fundCode,
        String fundName,
        String fundShortName,
        String fundType,
        String fundSubtype,
        String shareClass,
        Boolean isEtf,
        Boolean isIndex,
        String managementCompany,
        BigDecimal fundSize,
        BigDecimal managementFee,
        LocalDate inceptionDate,
        Boolean isActive
) {
    public static FundDto fromEntity(Fund f) {
        return new FundDto(
                f.getId(), f.getFundCode(), f.getFundName(), f.getFundShortName(),
                f.getFundType(), f.getFundSubtype(), f.getShareClass(),
                f.getIsEtf(), f.getIsIndex(), f.getManagementCompany(),
                f.getFundSize(), f.getManagementFee(), f.getInceptionDate(),
                f.getIsActive()
        );
    }
}
