package com.quantfund.api.service;

import com.quantfund.api.dto.FundScreenerDto;
import com.quantfund.api.dto.FundStatsDto;
import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.NavHistory;
import com.quantfund.common.entity.ScreeningMetrics;
import com.quantfund.common.repository.FundRepository;
import com.quantfund.common.repository.NavHistoryRepository;
import com.quantfund.common.repository.ScreeningMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FundService {

    private static final Logger log = LoggerFactory.getLogger(FundService.class);

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private NavHistoryRepository navHistoryRepository;

    @Autowired
    private ScreeningMetricsRepository metricsRepository;

    // ========== 基金查询 ==========

    public Page<Fund> searchFunds(String fundType, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fundSize").descending());

        if (keyword != null && !keyword.isBlank()) {
            return fundRepository.searchFunds(keyword, pageable);
        }
        if (fundType != null && !fundType.isBlank()) {
            return fundRepository.findByFundTypeAndActive(fundType, pageable);
        }
        return fundRepository.findActiveFunds(pageable);
    }

    @Cacheable(value = "funds", key = "#fundCode")
    public Fund getByFundCode(String fundCode) {
        return fundRepository.findByFundCode(fundCode).orElse(null);
    }

    public List<Fund> getEtfFunds() {
        return fundRepository.findByIsEtfTrue();
    }

    // ========== 净值数据 ==========

    public List<NavHistory> getNavHistory(String fundCode, LocalDate start, LocalDate end) {
        Fund fund = fundRepository.findByFundCode(fundCode).orElse(null);
        if (fund == null) return List.of();

        return navHistoryRepository.findByFundIdAndNavDateBetweenOrderByNavDateAsc(
                fund.getId(), start, end);
    }

    // ========== 基金筛选 ==========

    public List<FundScreenerDto> screenFunds(String fundType, BigDecimal minReturn1y,
                                              BigDecimal maxDrawdown1y, BigDecimal minSharpe,
                                              String sortBy, int topN) {
        LocalDate latestDate = metricsRepository.findLatestCalcDate();
        if (latestDate == null) {
            log.warn("No screening metrics available");
            return List.of();
        }

        List<ScreeningMetrics> allMetrics = metricsRepository.findByCalcDate(latestDate);
        List<FundScreenerDto> results = new ArrayList<>();

        for (ScreeningMetrics metrics : allMetrics) {
            // 应用筛选条件
            if (minReturn1y != null && metrics.getReturn1y() != null &&
                    metrics.getReturn1y().compareTo(minReturn1y) < 0) continue;

            if (maxDrawdown1y != null && metrics.getMaxDrawdown1y() != null &&
                    metrics.getMaxDrawdown1y().compareTo(maxDrawdown1y) > 0) continue;

            if (minSharpe != null && metrics.getSharpe1y() != null &&
                    metrics.getSharpe1y().compareTo(minSharpe) < 0) continue;

            // 加载基金基本信息
            Fund fund = fundRepository.findById(metrics.getFundId()).orElse(null);
            if (fund == null) continue;

            // 按类型筛选
            if (fundType != null && !fundType.isBlank() && !fundType.equals(fund.getFundType())) continue;

            results.add(FundScreenerDto.fromMetricsAndFund(metrics, fund));
        }

        // 排序
        Comparator<FundScreenerDto> comparator = switch (sortBy != null ? sortBy : "sharpe") {
            case "return_1y" -> Comparator.comparing(FundScreenerDto::return1y,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "max_drawdown" -> Comparator.comparing(FundScreenerDto::maxDrawdown1y,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "volatility" -> Comparator.comparing(FundScreenerDto::volatility1y,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(FundScreenerDto::sharpe1y,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };

        results.sort(comparator);
        return results.stream().limit(topN).toList();
    }

    // ========== 辅助 ==========

    @Cacheable("companies")
    public List<String> getDistinctCompanies() {
        return fundRepository.findDistinctCompanies();
    }

    @Cacheable("fundTypes")
    public List<String> getDistinctTypes() {
        return fundRepository.findDistinctTypes();
    }

    public FundStatsDto getStats() {
        return new FundStatsDto(
                fundRepository.countActiveFunds(),
                fundRepository.count(),
                fundRepository.findByIsEtfTrue().size()
        );
    }
}
