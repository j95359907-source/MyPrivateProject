package com.quantfund.api.controller;

import com.quantfund.data.scheduler.DataSyncJob;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
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
    @Operation(summary = "手动触发净值同步")
    public ResponseEntity<Map<String, String>> syncNav() {
        new Thread(() -> {
            try {
                Method m = DataSyncJob.class.getDeclaredMethod("syncNavForRecentFunds");
                m.setAccessible(true);
                m.invoke(syncJob);
            } catch (Exception e) {
                syncJob.syncNavDataEvening();
            }
        }).start();
        return ResponseEntity.ok(Map.of("status", "started", "task", "nav-data"));
    }
}
