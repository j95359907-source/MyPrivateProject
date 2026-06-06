package com.quantfund.api.controller;

import com.quantfund.api.service.BacktestService;
import com.quantfund.common.entity.BacktestResult;
import com.quantfund.common.entity.BacktestRun;
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
@RequestMapping("/api/v1/backtests")
@Tag(name = "策略回测", description = "回测创建、执行、查询、对比")
public class BacktestController {

    @Autowired
    private BacktestService backtestService;

    @PostMapping
    @Operation(summary = "创建并运行回测")
    public ResponseEntity<Map<String, Object>> runBacktest(@RequestBody BacktestRequest req) {
        BacktestRun run = backtestService.createBacktest(
                req.strategyName(), req.strategyType(), req.parameters(),
                req.fundCodes(), req.startDate(), req.endDate(), req.initialCapital());
        return ResponseEntity.ok(Map.of("id", run.getId(), "status", run.getStatus()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询回测状态")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long id) {
        Optional<BacktestRun> run = backtestService.getBacktestStatus(id);
        if (run.isEmpty()) return ResponseEntity.notFound().build();

        BacktestRun r = run.get();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", r.getId());
        resp.put("strategyId", r.getStrategyId());
        resp.put("status", r.getStatus());
        resp.put("startDate", r.getStartDate().toString());
        resp.put("endDate", r.getEndDate().toString());
        resp.put("initialCapital", r.getInitialCapital());
        resp.put("startedAt", r.getStartedAt());
        resp.put("completedAt", r.getCompletedAt());
        resp.put("durationSeconds", r.getDurationSeconds());
        resp.put("errorMessage", r.getErrorMessage());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "获取回测结果")
    public ResponseEntity<Map<String, Object>> getResults(@PathVariable Long id) {
        Optional<BacktestResult> opt = backtestService.getBacktestResult(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BacktestResult r = opt.get();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("backtestId", r.getBacktestId());
        resp.put("totalReturn", r.getTotalReturn());
        resp.put("annualReturn", r.getAnnualReturn());
        resp.put("volatility", r.getVolatility());
        resp.put("sharpeRatio", r.getSharpeRatio());
        resp.put("sortinoRatio", r.getSortinoRatio());
        resp.put("calmarRatio", r.getCalmarRatio());
        resp.put("maxDrawdown", r.getMaxDrawdown());
        resp.put("winRate", r.getWinRate());
        resp.put("totalTrades", r.getTotalTrades());
        resp.put("alpha", r.getAlpha());
        resp.put("beta", r.getBeta());
        resp.put("equityCurve", r.getEquityCurve());
        resp.put("tradeLog", r.getTradeLog());
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    @Operation(summary = "回测列表")
    public ResponseEntity<List<Map<String, Object>>> listBacktests(
            @RequestParam(defaultValue = "COMPLETED") String status) {
        List<BacktestRun> runs = backtestService.listCompletedBacktests();
        List<Map<String, Object>> list = new ArrayList<>();
        for (BacktestRun r : runs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("strategyId", r.getStrategyId());
            m.put("runName", r.getRunName());
            m.put("status", r.getStatus());
            m.put("startDate", r.getStartDate().toString());
            m.put("endDate", r.getEndDate().toString());
            m.put("completedAt", r.getCompletedAt());
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除回测")
    public ResponseEntity<Void> deleteBacktest(@PathVariable Long id) {
        backtestService.getBacktestStatus(id).ifPresent(r -> {
            // cleanup logic
        });
        return ResponseEntity.noContent().build();
    }

    public record BacktestRequest(
            String strategyName, String strategyType,
            Map<String, Object> parameters,
            List<String> fundCodes,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            BigDecimal initialCapital
    ) {}
}
