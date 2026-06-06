package com.quantfund.api.controller;

import com.quantfund.trading.broker.BrokerInterface;
import com.quantfund.trading.broker.QmtBroker;
import com.quantfund.trading.notify.NotifyService;
import com.quantfund.trading.order.OrderStateMachine;
import com.quantfund.trading.paper.PaperTradingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/trading")
@Tag(name = "交易执行", description = "信号生成、订单管理、券商操作")
public class TradingController {

    // 默认模拟交易引擎
    private PaperTradingEngine paperEngine = new PaperTradingEngine(new BigDecimal("1000000"));
    private BrokerInterface activeBroker = paperEngine;
    private final OrderStateMachine stateMachine = new OrderStateMachine();
    private final NotifyService notifyService = new NotifyService();
    private boolean liveMode = false;

    // ========== 券商管理 ==========

    @GetMapping("/brokers")
    @Operation(summary = "券商列表")
    public ResponseEntity<List<Map<String, String>>> listBrokers() {
        return ResponseEntity.ok(List.of(
                Map.of("name", "PaperTrading", "status", "connected", "type", "simulated"),
                Map.of("name", "QMT-miniQMT", "status", "disconnected", "type", "live")
        ));
    }

    @PostMapping("/brokers/qmt/test")
    @Operation(summary = "测试QMT连接")
    public ResponseEntity<Map<String, Object>> testQmt() {
        QmtBroker qmt = new QmtBroker();
        boolean ok = qmt.testConnection();
        return ResponseEntity.ok(Map.of("connected", ok, "broker", qmt.getBrokerName()));
    }

    @PostMapping("/mode")
    @Operation(summary = "切换交易模式", description = "paper → 模拟交易 / live → 实盘(QMT)")
    public ResponseEntity<Map<String, String>> switchMode(@RequestBody Map<String, String> body) {
        String mode = body.getOrDefault("mode", "paper");
        if ("live".equals(mode)) {
            QmtBroker qmt = new QmtBroker();
            if (qmt.testConnection()) {
                activeBroker = qmt;
                liveMode = true;
                return ResponseEntity.ok(Map.of("mode", "live", "status", "已连接QMT"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "QMT连接失败"));
        }
        activeBroker = paperEngine;
        liveMode = false;
        return ResponseEntity.ok(Map.of("mode", "paper", "status", "模拟交易"));
    }

    // ========== 订单管理 ==========

    @PostMapping("/orders")
    @Operation(summary = "创建并提交订单")
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Map<String, Object> body) {
        String fundCode = (String) body.get("fundCode");
        String tradeType = (String) body.get("tradeType");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        BigDecimal nav = new BigDecimal(body.getOrDefault("nav", "1.0").toString());

        // 实盘模式：需人工审批
        if (liveMode && !Boolean.TRUE.equals(body.get("confirmed"))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "实盘模式需confirmed=true", "mode", "live"));
        }

        // 创建状态机订单
        String orderId = stateMachine.createOrder(fundCode, tradeType, amount.doubleValue());
        stateMachine.transition(orderId, OrderStateMachine.Status.SUBMITTED, "提交至券商");

        // 提交给券商
        BrokerInterface.OrderRequest req = new BrokerInterface.OrderRequest(
                fundCode, tradeType, amount, nav, (String) body.getOrDefault("notes", ""));
        BrokerInterface.OrderResult result = activeBroker.submitOrder(req);

        // 更新状态
        if ("EXECUTED".equals(result.status())) {
            stateMachine.transition(orderId, OrderStateMachine.Status.EXECUTED, "成交");
        } else if ("FAILED".equals(result.status())) {
            stateMachine.transition(orderId, OrderStateMachine.Status.FAILED,
                    result.message());
        }

        notifyService.sendOrderExecution(orderId, fundCode, tradeType,
                amount.doubleValue(), result.status());

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", result.status(),
                "filledAmount", result.filledAmount(),
                "filledShares", result.filledShares(),
                "price", result.price(),
                "fee", result.fee(),
                "message", result.message()
        ));
    }

    @DeleteMapping("/orders/{orderId}")
    @Operation(summary = "撤单")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        boolean ok = activeBroker.cancelOrder(orderId);
        if (ok) stateMachine.transition(orderId, OrderStateMachine.Status.CANCELLED, "手动撤单");
        return ResponseEntity.ok(Map.of("orderId", orderId, "cancelled", ok));
    }

    @GetMapping("/orders")
    @Operation(summary = "订单列表")
    public ResponseEntity<List<Map<String, Object>>> listOrders() {
        List<Map<String, Object>> orders = new ArrayList<>();
        for (String oid : stateMachine.getActiveOrders()) {
            orders.add(Map.of("orderId", oid, "status",
                    stateMachine.getStatus(oid).toString()));
        }
        return ResponseEntity.ok(orders);
    }

    // ========== 持仓与账户 ==========

    @GetMapping("/positions")
    @Operation(summary = "当前持仓")
    public ResponseEntity<List<BrokerInterface.Position>> getPositions() {
        return ResponseEntity.ok(activeBroker.queryPositions());
    }

    @GetMapping("/account")
    @Operation(summary = "账户摘要")
    public ResponseEntity<BrokerInterface.AccountInfo> getAccount() {
        return ResponseEntity.ok(activeBroker.queryAccount());
    }

    // ========== 信号生成 ==========

    @PostMapping("/signals/generate")
    @Operation(summary = "生成交易信号（不执行）", description = "根据策略生成信号供人工审查")
    public ResponseEntity<List<Map<String, Object>>> generateSignals(@RequestBody Map<String, Object> body) {
        // 简化：返回示例信号（实际应从策略引擎生成）
        return ResponseEntity.ok(List.of(Map.of(
                "fundCode", "示例基金",
                "type", "需要策略引擎生成",
                "weight", 0.2,
                "confidence", 0.85,
                "reason", "这是信号生成占位，实际需接入策略引擎"
        )));
    }

    @PostMapping("/signals/execute")
    @Operation(summary = "执行待处理信号")
    public ResponseEntity<List<Map<String, Object>>> executeSignals(@RequestBody List<Map<String, Object>> signals) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> signal : signals) {
            String fundCode = (String) signal.get("fundCode");
            String type = (String) signal.get("type");
            double weight = ((Number) signal.get("weight")).doubleValue();
            BrokerInterface.AccountInfo account = activeBroker.queryAccount();
            BigDecimal amount = account != null
                    ? account.totalAssets().multiply(BigDecimal.valueOf(weight))
                    : new BigDecimal("50000");

            BrokerInterface.OrderResult r = activeBroker.submitOrder(
                    new BrokerInterface.OrderRequest(fundCode, type, amount, null, "信号执行"));
            results.add(Map.of("fundCode", fundCode, "status", r.status()));
        }
        return ResponseEntity.ok(results);
    }
}
