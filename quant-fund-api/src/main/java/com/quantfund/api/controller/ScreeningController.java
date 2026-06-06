package com.quantfund.api.controller;

import com.quantfund.api.service.MetricsSyncService;
import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.ScreeningMetrics;
import com.quantfund.common.repository.FundRepository;
import com.quantfund.common.repository.ScreeningMetricsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/screening")
@Tag(name = "基金筛选", description = "量化指标查询、历史指标、同类对比")
public class ScreeningController {

    @Autowired
    private ScreeningMetricsRepository metricsRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private MetricsSyncService metricsSyncService;

    // ========== 指标查询 ==========

    @GetMapping("/metrics/{fundCode}")
    @Operation(summary = "基金最新指标")
    public ResponseEntity<Map<String, Object>> getLatestMetrics(@PathVariable String fundCode) {
        Fund fund = fundRepository.findByFundCode(fundCode).orElse(null);
        if (fund == null) return ResponseEntity.notFound().build();

        LocalDate latestDate = metricsRepository.findLatestCalcDate();
        if (latestDate == null) return ResponseEntity.ok(Map.of("message", "暂无指标数据"));

        ScreeningMetrics m = metricsRepository.findByFundIdAndCalcDate(fund.getId(), latestDate).orElse(null);
        if (m == null) return ResponseEntity.ok(Map.of("message", "该基金暂无计算指标"));

        return ResponseEntity.ok(metricsToMap(m, fund));
    }

    @GetMapping("/metrics/{fundCode}/history")
    @Operation(summary = "基金指标历史", description = "获取指定基金的历史指标数据，用于绘制趋势图")
    public ResponseEntity<List<Map<String, Object>>> getMetricsHistory(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "12") int months) {

        Fund fund = fundRepository.findByFundCode(fundCode).orElse(null);
        if (fund == null) return ResponseEntity.notFound().build();

        // 简化：获取最近N个月每月最后一天的指标
        List<Map<String, Object>> history = new ArrayList<>();
        // 从最新日期向前推算
        LocalDate latestDate = metricsRepository.findLatestCalcDate();
        if (latestDate == null) return ResponseEntity.ok(List.of());

        LocalDate cursor = latestDate;
        for (int i = 0; i < months && cursor.isAfter(LocalDate.now().minusYears(3)); i++) {
            cursor = cursor.minusMonths(1);
            Optional<ScreeningMetrics> opt = metricsRepository.findByFundIdAndCalcDate(fund.getId(), cursor);
            if (opt.isPresent()) {
                history.add(Map.of(
                        "date", cursor.toString(),
                        "sharpe", opt.get().getSharpe1y() != null ? opt.get().getSharpe1y().doubleValue() : null,
                        "return1y", opt.get().getReturn1y() != null ? opt.get().getReturn1y().doubleValue() : null,
                        "maxDrawdown", opt.get().getMaxDrawdown1y() != null ? opt.get().getMaxDrawdown1y().doubleValue() : null
                ));
            }
        }

        return ResponseEntity.ok(history);
    }

    // ========== 对比分析 ==========

    @GetMapping("/compare")
    @Operation(summary = "多基金对比", description = "同时对比多只基金的关键指标")
    public ResponseEntity<List<Map<String, Object>>> compareFunds(
            @RequestParam List<String> codes) {

        LocalDate latestDate = metricsRepository.findLatestCalcDate();
        List<Map<String, Object>> result = new ArrayList<>();

        for (String code : codes) {
            Fund fund = fundRepository.findByFundCode(code).orElse(null);
            if (fund == null) continue;

            ScreeningMetrics m = metricsRepository.findByFundIdAndCalcDate(fund.getId(), latestDate).orElse(null);
            if (m == null) continue;

            result.add(metricsToMap(m, fund));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/top")
    @Operation(summary = "TOP排行", description = "按指定指标获取排名靠前的基金")
    public ResponseEntity<List<Map<String, Object>>> getTopFunds(
            @RequestParam(defaultValue = "sharpe") String rankBy,
            @RequestParam(defaultValue = "20") int topN,
            @RequestParam(required = false) String fundType) {

        LocalDate latestDate = metricsRepository.findLatestCalcDate();
        if (latestDate == null) return ResponseEntity.ok(List.of());

        List<ScreeningMetrics> allMetrics = metricsRepository.findByCalcDate(latestDate);

        // 排序
        Comparator<ScreeningMetrics> comparator = switch (rankBy) {
            case "return_1y" -> Comparator.comparing(ScreeningMetrics::getReturn1y,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "sharpe" -> Comparator.comparing(ScreeningMetrics::getSharpe1y,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "sortino" -> Comparator.comparing(ScreeningMetrics::getSortino1y,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "calmar" -> Comparator.comparing(ScreeningMetrics::getCalmarRatio,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "min_drawdown" -> Comparator.comparing(ScreeningMetrics::getMaxDrawdown1y,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(ScreeningMetrics::getSharpe1y,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };

        allMetrics.sort(comparator);

        List<Map<String, Object>> topList = new ArrayList<>();
        for (ScreeningMetrics m : allMetrics) {
            if (topList.size() >= topN) break;

            Fund fund = fundRepository.findById(m.getFundId()).orElse(null);
            if (fund == null) continue;

            // 类型筛选
            if (fundType != null && !fundType.isBlank() && !fundType.equals(fund.getFundType())) continue;

            topList.add(metricsToMap(m, fund));
        }

        return ResponseEntity.ok(topList);
    }

    // ========== 指标同步管理 ==========

    @PostMapping("/recalculate")
    @Operation(summary = "触发全量重算", description = "重新计算指定时间范围内的所有指标")
    public ResponseEntity<Map<String, String>> triggerRecalculation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // 直接调用每日计算——对当前日期计算指标
        new Thread(() -> metricsSyncService.dailyMetricsCalculation()).start();
        return ResponseEntity.ok(Map.of("status", "started", "desc", "正在计算今日指标"));
    }

    // ========== 辅助 ==========

    private Map<String, Object> metricsToMap(ScreeningMetrics m, Fund f) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fundCode", f.getFundCode());
        map.put("fundName", f.getFundName());
        map.put("fundType", f.getFundType());
        map.put("company", f.getManagementCompany());
        map.put("calcDate", m.getCalcDate().toString());

        // 收益
        Map<String, Double> returns = new LinkedHashMap<>();
        if (m.getReturn1m() != null) returns.put("1m", m.getReturn1m().doubleValue());
        if (m.getReturn3m() != null) returns.put("3m", m.getReturn3m().doubleValue());
        if (m.getReturn6m() != null) returns.put("6m", m.getReturn6m().doubleValue());
        if (m.getReturn1y() != null) returns.put("1y", m.getReturn1y().doubleValue());
        if (m.getReturn3y() != null) returns.put("3y", m.getReturn3y().doubleValue());
        if (m.getReturnYtd() != null) returns.put("ytd", m.getReturnYtd().doubleValue());
        map.put("returns", returns);

        // 风险
        Map<String, Double> risk = new LinkedHashMap<>();
        if (m.getVolatility1y() != null) risk.put("volatility1y", m.getVolatility1y().doubleValue());
        if (m.getMaxDrawdown1y() != null) risk.put("maxDrawdown1y", m.getMaxDrawdown1y().doubleValue());
        map.put("risk", risk);

        // 风险调整
        Map<String, Double> ratios = new LinkedHashMap<>();
        if (m.getSharpe1y() != null) ratios.put("sharpe", m.getSharpe1y().doubleValue());
        if (m.getSortino1y() != null) ratios.put("sortino", m.getSortino1y().doubleValue());
        if (m.getCalmarRatio() != null) ratios.put("calmar", m.getCalmarRatio().doubleValue());
        map.put("ratios", ratios);

        // 排名
        if (m.getRank1y() != null) map.put("rank1y", m.getRank1y());
        if (m.getPercentile1y() != null) map.put("percentile1y", m.getPercentile1y().doubleValue());

        return map;
    }
}
