package com.quantfund.api.controller;

import com.quantfund.api.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/backtest/{id}/report")
    @Operation(summary = "导出回测HTML报告")
    public ResponseEntity<String> backtestReport(@PathVariable Long id) {
        String html = exportService.generateBacktestReport(id);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @GetMapping("/backtest/{id}/trades.csv")
    @Operation(summary = "导出交易记录CSV")
    public ResponseEntity<String> backtestTradesCsv(@PathVariable Long id) {
        String csv = exportService.exportTradeCsv(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=backtest-" + id + "-trades.csv")
                .body(csv);
    }
}
