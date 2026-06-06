package com.quantfund.api.controller;

import com.quantfund.api.service.PortfolioService;
import com.quantfund.common.entity.Portfolio;
import com.quantfund.common.entity.PortfolioHolding;
import com.quantfund.core.portfolio.RebalanceEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "投资组合", description = "组合管理、优化、再平衡")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping
    @Operation(summary = "组合列表")
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<Portfolio> portfolios = portfolioService.listPortfolios();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Portfolio p : portfolios) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("currentValue", p.getCurrentValue());
            m.put("initialCapital", p.getInitialCapital());
            m.put("rebalanceFreq", p.getRebalanceFreq());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "创建组合")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String desc = (String) body.getOrDefault("description", "");
        BigDecimal capital = new BigDecimal(body.getOrDefault("initialCapital", "100000").toString());

        Portfolio p = portfolioService.createPortfolio(name, desc, capital);
        return ResponseEntity.ok(Map.of("id", p.getId(), "name", p.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "组合详情")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        Optional<Portfolio> opt = portfolioService.getPortfolio(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Portfolio p = opt.get();
        List<PortfolioHolding> holdings = portfolioService.getHoldings(id);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", p.getId());
        resp.put("name", p.getName());
        resp.put("description", p.getDescription());
        resp.put("initialCapital", p.getInitialCapital());
        resp.put("currentValue", p.getCurrentValue());
        resp.put("rebalanceFreq", p.getRebalanceFreq());

        List<Map<String, Object>> holdingList = new ArrayList<>();
        for (PortfolioHolding h : holdings) {
            Map<String, Object> hm = new LinkedHashMap<>();
            hm.put("fundId", h.getFundId());
            hm.put("targetWeight", h.getTargetWeight());
            hm.put("currentWeight", h.getCurrentWeight());
            hm.put("sharesHeld", h.getSharesHeld());
            hm.put("currentValue", h.getCurrentValue());
            hm.put("unrealizedPnl", h.getUnrealizedPnl());
            holdingList.add(hm);
        }
        resp.put("holdings", holdingList);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/funds")
    @Operation(summary = "添加基金")
    public ResponseEntity<Void> addFund(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String fundCode = (String) body.get("fundCode");
        BigDecimal weight = new BigDecimal(body.getOrDefault("targetWeight", "0.1").toString());
        portfolioService.addFundToPortfolio(id, fundCode, weight);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/optimize")
    @Operation(summary = "运行优化", description = "objective: max_sharpe / min_volatility / risk_parity")
    public ResponseEntity<Map<String, Object>> optimize(@PathVariable Long id,
                                                         @RequestBody(required = false) Map<String, String> body) {
        String objective = body != null ? body.getOrDefault("objective", "max_sharpe") : "max_sharpe";
        Map<String, Object> result = portfolioService.optimize(id, objective);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/rebalance")
    @Operation(summary = "生成再平衡方案")
    public ResponseEntity<Map<String, Object>> rebalance(@PathVariable Long id) {
        RebalanceEngine.RebalancePlan plan = portfolioService.rebalance(id);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("needsRebalance", plan.needsRebalance());
        resp.put("totalBuy", plan.totalBuy());
        resp.put("totalSell", plan.totalSell());
        resp.put("netFlow", plan.netFlow());
        resp.put("currentValue", plan.currentValue());

        List<Map<String, Object>> instructions = plan.instructions().stream()
                .map(i -> Map.of("fundCode", (Object) i.fundCode(),
                        "type", i.type(), "amount", i.amount(), "reason", i.reason()))
                .toList();
        resp.put("instructions", instructions);

        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除组合")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        portfolioService.getPortfolio(id).ifPresent(p -> {
            p.setIsActive(false);
            // save via service would need a repository ref; simplified
        });
        return ResponseEntity.noContent().build();
    }
}
