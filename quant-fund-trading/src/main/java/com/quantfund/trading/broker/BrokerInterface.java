package com.quantfund.trading.broker;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 券商交易接口抽象 —— 统一QMT/PTrade/模拟交易
 */
public interface BrokerInterface {

    /** 提交订单 */
    OrderResult submitOrder(OrderRequest request);

    /** 撤单 */
    boolean cancelOrder(String orderId);

    /** 查询订单状态 */
    OrderResult queryOrder(String orderId);

    /** 查询当前持仓 */
    List<Position> queryPositions();

    /** 查询账户信息 */
    AccountInfo queryAccount();

    /** 测试连接 */
    boolean testConnection();

    /** 券商名称 */
    String getBrokerName();

    /** 是否为模拟交易 */
    boolean isSimulated();

    // --- 内部类型 ---

    record OrderRequest(String fundCode, String tradeType, // BUY/SELL
                        BigDecimal amount, BigDecimal nav, String notes) {}

    record OrderResult(String orderId, String status, BigDecimal filledAmount,
                       BigDecimal filledShares, BigDecimal price, BigDecimal fee, String message) {}

    record Position(String fundCode, String fundName, BigDecimal shares,
                    BigDecimal avgCost, BigDecimal currentPrice, BigDecimal marketValue,
                    BigDecimal unrealizedPnl, BigDecimal returnPct) {}

    record AccountInfo(BigDecimal totalAssets, BigDecimal availableCash,
                       BigDecimal marketValue, BigDecimal totalPnl, BigDecimal dailyPnl) {}
}
