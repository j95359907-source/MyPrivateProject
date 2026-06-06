package com.quantfund.trading.broker;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 迅投QMT miniQMT模式交易接口
 *
 * miniQMT通过本地TCP端口暴露API：
 * - 默认端口：15555（登录端口）/ 15556（交易端口）
 * - 协议：HTTP REST / TCP Socket（具体取决于版本）
 * - 本实现通过HTTP调用miniQMT的本地API
 *
 * 前置条件：QMT客户端运行中 + miniQMT模式已启用
 */
public class QmtBroker implements BrokerInterface {

    private static final Logger log = LoggerFactory.getLogger(QmtBroker.class);

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper json = new ObjectMapper();
    private boolean connected = false;

    public QmtBroker(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public QmtBroker() {
        this("127.0.0.1", 15556); // 默认本地miniQMT交易端口
    }

    @Override
    public boolean testConnection() {
        try {
            Request req = new Request.Builder().url(baseUrl + "/api/status").build();
            try (Response resp = httpClient.newCall(req).execute()) {
                connected = resp.isSuccessful();
                return connected;
            }
        } catch (Exception e) {
            log.warn("QMT连接失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public OrderResult submitOrder(OrderRequest req) {
        if (!connected && !testConnection()) {
            return new OrderResult("", "FAILED", BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "QMT未连接");
        }

        try {
            // QMT下单接口（具体参数格式需根据券商SDK文档调整）
            Map<String, Object> orderParams = Map.of(
                    "code", req.fundCode(),
                    "type", req.tradeType().equals("BUY") ? 1 : 2, // 1=买入 2=卖出
                    "amount", req.amount().doubleValue(),
                    "price", req.nav() != null ? req.nav().doubleValue() : 0
            );

            String body = json.writeValueAsString(orderParams);
            Request httpReq = new Request.Builder()
                    .url(baseUrl + "/api/order/submit")
                    .post(RequestBody.create(body, MediaType.parse("application/json")))
                    .build();

            try (Response resp = httpClient.newCall(httpReq).execute()) {
                if (!resp.isSuccessful()) {
                    return new OrderResult("", "FAILED", BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            "HTTP " + resp.code());
                }
                Map<String, Object> result = json.readValue(resp.body().string(), Map.class);
                return new OrderResult(
                        (String) result.getOrDefault("orderId", ""),
                        "SUBMITTED",
                        req.amount(),
                        BigDecimal.ZERO,
                        req.nav(),
                        BigDecimal.ZERO,
                        "已提交至QMT"
                );
            }
        } catch (Exception e) {
            log.error("QMT下单失败", e);
            return new OrderResult("", "FAILED", BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    e.getMessage());
        }
    }

    @Override
    public boolean cancelOrder(String orderId) {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/api/order/cancel?orderId=" + orderId)
                    .post(RequestBody.create("", null))
                    .build();
            try (Response resp = httpClient.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            log.error("QMT撤单失败", e);
            return false;
        }
    }

    @Override
    public OrderResult queryOrder(String orderId) {
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/api/order/query?orderId=" + orderId).build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return null;
                Map<String, Object> m = json.readValue(resp.body().string(), Map.class);
                return new OrderResult(orderId, (String) m.get("status"),
                        BigDecimal.valueOf(((Number) m.get("filledAmount")).doubleValue()),
                        BigDecimal.valueOf(((Number) m.get("filledShares")).doubleValue()),
                        BigDecimal.valueOf(((Number) m.get("price")).doubleValue()),
                        BigDecimal.ZERO, "");
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Position> queryPositions() {
        // 通过QMT查询实际持仓
        return List.of(); // 需根据实际SDK实现
    }

    @Override
    public AccountInfo queryAccount() {
        try {
            Request req = new Request.Builder().url(baseUrl + "/api/account").build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful()) return null;
                Map<String, Object> m = json.readValue(resp.body().string(), Map.class);
                return new AccountInfo(
                        BigDecimal.valueOf(((Number) m.get("totalAssets")).doubleValue()),
                        BigDecimal.valueOf(((Number) m.get("availableCash")).doubleValue()),
                        BigDecimal.valueOf(((Number) m.get("marketValue")).doubleValue()),
                        BigDecimal.valueOf(((Number) m.get("totalPnl")).doubleValue()),
                        BigDecimal.valueOf(((Number) m.get("dailyPnl")).doubleValue())
                );
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override public String getBrokerName() { return "QMT-miniQMT"; }
    @Override public boolean isSimulated() { return false; }
}
