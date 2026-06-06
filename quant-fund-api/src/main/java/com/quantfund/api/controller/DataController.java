package com.quantfund.api.controller;

import com.quantfund.data.scheduler.DataSyncJob;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/data")
public class DataController {

    @Autowired
    private DataSyncJob syncJob;

    @PostMapping("/sync/fund-list")
    @Operation(summary = "手动触发基金列表同步")
    public ResponseEntity<Map<String, String>> syncFundList() {
        new Thread(() -> syncJob.syncFundList()).start();
        return ResponseEntity.ok(Map.of("status", "started", "task", "fund-list"));
    }

    @PostMapping("/sync/nav")
    @Operation(summary = "手动触发净值同步（默认：1年/300只/80ms）")
    public ResponseEntity<Map<String, String>> syncNav(
            @RequestParam(defaultValue = "365") int days,
            @RequestParam(defaultValue = "300") int limit,
            @RequestParam(defaultValue = "80") int delay) {
        new Thread(() -> syncJob.syncNavForFunds(days, limit, delay)).start();
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "days", String.valueOf(days),
                "limit", String.valueOf(limit),
                "delayMs", String.valueOf(delay)
        ));
    }

    @GetMapping("/nav/progress")
    @Operation(summary = "查看净值同步进度（日志可见）")
    public ResponseEntity<Map<String, String>> navProgress() {
        return ResponseEntity.ok(Map.of("check", "查看后端日志: grep '净值同步' /tmp/backend*.log"));
    }
}
