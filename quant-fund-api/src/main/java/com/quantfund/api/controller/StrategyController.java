package com.quantfund.api.controller;

import com.quantfund.common.entity.Strategy;
import com.quantfund.common.repository.StrategyRepository;
import com.quantfund.core.strategy.StrategyRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/strategies")
@Tag(name = "策略管理", description = "策略 CRUD + 模板列表")
public class StrategyController {

    @Autowired
    private StrategyRepository strategyRepo;

    @GetMapping("/templates")
    @Operation(summary = "策略模板列表", description = "获取所有内置策略类型及其默认参数")
    public ResponseEntity<List<StrategyRegistry.StrategyTypeInfo>> listTemplates() {
        return ResponseEntity.ok(StrategyRegistry.listTypes());
    }

    @GetMapping
    @Operation(summary = "已保存策略列表")
    public ResponseEntity<List<Strategy>> listStrategies() {
        return ResponseEntity.ok(strategyRepo.findByIsActiveTrue());
    }

    @GetMapping("/{id}")
    @Operation(summary = "策略详情")
    public ResponseEntity<Strategy> getStrategy(@PathVariable Long id) {
        return strategyRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "创建策略")
    public ResponseEntity<Strategy> createStrategy(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("strategyType");
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.getOrDefault("parameters", Map.of());

        // 验证参数
        var tempStrategy = StrategyRegistry.create(type);
        tempStrategy.setParameters(params);
        if (!tempStrategy.validateParameters(params)) {
            return ResponseEntity.badRequest().build();
        }

        Strategy entity = new Strategy();
        entity.setName(name);
        entity.setStrategyType(type);
        entity.setParameters(params.toString()); // 转为JSON字符串存储
        entity.setMaxHoldings((Integer) params.getOrDefault("topN", 5));
        entity.setTargetUniverse("ALL");
        entity.setDescription(tempStrategy.getDescription());
        entity = strategyRepo.save(entity);

        return ResponseEntity.ok(entity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新策略")
    public ResponseEntity<Strategy> updateStrategy(@PathVariable Long id,
                                                    @RequestBody Map<String, Object> body) {
        return strategyRepo.findById(id).map(entity -> {
            if (body.containsKey("parameters")) {
                entity.setParameters(body.get("parameters").toString());
            }
            if (body.containsKey("name")) {
                entity.setName((String) body.get("name"));
            }
            return ResponseEntity.ok(strategyRepo.save(entity));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除策略")
    public ResponseEntity<Void> deleteStrategy(@PathVariable Long id) {
        strategyRepo.findById(id).ifPresent(s -> {
            s.setIsActive(false);
            strategyRepo.save(s);
        });
        return ResponseEntity.noContent().build();
    }
}
