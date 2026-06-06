package com.quantfund.trading.order;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单状态机 —— 管理订单生命周期
 *
 * PENDING → SUBMITTED → EXECUTED
 *                     → FAILED
 *                     → CANCELLED (需手动取消)
 * PENDING → CANCELLED (提交前取消)
 */
public class OrderStateMachine {

    public enum Status {
        PENDING,     // 待提交（信号已生成，等待确认）
        SUBMITTED,   // 已提交券商
        EXECUTED,    // 已成交
        PARTIAL,     // 部分成交
        FAILED,      // 失败
        CANCELLED    // 已取消
    }

    private static final Set<Status> TERMINAL_STATES = EnumSet.of(
            Status.EXECUTED, Status.FAILED, Status.CANCELLED);

    private static final Map<Status, Set<Status>> VALID_TRANSITIONS = Map.of(
            Status.PENDING,   EnumSet.of(Status.SUBMITTED, Status.CANCELLED),
            Status.SUBMITTED, EnumSet.of(Status.EXECUTED, Status.PARTIAL, Status.FAILED, Status.CANCELLED),
            Status.PARTIAL,   EnumSet.of(Status.EXECUTED, Status.FAILED, Status.CANCELLED)
    );

    private record OrderState(Status status, LocalDateTime timestamp, String note) {}

    private final Map<String, List<OrderState>> orderHistory = new ConcurrentHashMap<>();

    /** 创建订单并记录初始状态 */
    public String createOrder(String fundCode, String tradeType, double amount) {
        String orderId = UUID.randomUUID().toString().substring(0, 12);
        List<OrderState> states = new ArrayList<>();
        states.add(new OrderState(Status.PENDING, LocalDateTime.now(),
                String.format("创建%s订单: %s ¥%.2f", tradeType, fundCode, amount)));
        orderHistory.put(orderId, states);
        return orderId;
    }

    /** 状态转换 */
    public synchronized boolean transition(String orderId, Status newStatus, String note) {
        List<OrderState> history = orderHistory.get(orderId);
        if (history == null) return false;

        Status current = history.get(history.size() - 1).status();
        Set<Status> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(newStatus)) {
            return false; // 非法状态转换
        }

        history.add(new OrderState(newStatus, LocalDateTime.now(), note));
        return true;
    }

    /** 获取当前状态 */
    public Status getStatus(String orderId) {
        List<OrderState> history = orderHistory.get(orderId);
        if (history == null) return null;
        return history.get(history.size() - 1).status();
    }

    /** 是否为终态 */
    public boolean isTerminal(String orderId) {
        Status s = getStatus(orderId);
        return s != null && TERMINAL_STATES.contains(s);
    }

    /** 获取完整状态历史 */
    public List<OrderState> getHistory(String orderId) {
        return orderHistory.getOrDefault(orderId, List.of());
    }

    /** 获取所有活跃订单（非终态） */
    public List<String> getActiveOrders() {
        return orderHistory.entrySet().stream()
                .filter(e -> !TERMINAL_STATES.contains(
                        e.getValue().get(e.getValue().size()-1).status()))
                .map(Map.Entry::getKey).toList();
    }
}
