package com.quantfund.trading.paper;

import com.quantfund.trading.broker.BrokerInterface;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟交易引擎 —— 按每日NAV模拟成交，跟踪虚拟持仓和盈亏
 *
 * 用于在无券商账户时先验证策略效果
 */
public class PaperTradingEngine implements BrokerInterface {

    private BigDecimal cash;
    private final Map<String, PaperPosition> positions = new ConcurrentHashMap<>();
    private final List<OrderResult> orderHistory = new ArrayList<>();
    private final List<PriceRecord> priceHistory = new ArrayList<>();
    private int orderCounter = 0;

    public PaperTradingEngine(BigDecimal initialCapital) {
        this.cash = initialCapital;
    }

    @Override
    public OrderResult submitOrder(OrderRequest req) {
        orderCounter++;
        String orderId = "PAPER-" + orderCounter;

        if ("BUY".equals(req.tradeType())) {
            // 扣除申购费
            BigDecimal fee = req.amount().multiply(new BigDecimal("0.0015"))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal netAmount = req.amount().subtract(fee);

            if (netAmount.compareTo(cash) > 0) {
                return new OrderResult(orderId, "FAILED", BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, fee,
                        "资金不足：需要¥" + netAmount + "，可用¥" + cash);
            }

            BigDecimal shares = netAmount.divide(req.nav(), 4, RoundingMode.HALF_DOWN);
            cash = cash.subtract(netAmount);

            positions.merge(req.fundCode(), new PaperPosition(
                    req.fundCode(), shares, req.nav(), req.nav(), BigDecimal.ZERO), (old, n) -> {
                BigDecimal totalShares = old.shares.add(shares);
                BigDecimal totalCost = old.shares.multiply(old.avgCost)
                        .add(shares.multiply(req.nav()));
                BigDecimal newAvgCost = totalCost.divide(totalShares, 4, RoundingMode.HALF_UP);
                return new PaperPosition(req.fundCode(), totalShares, newAvgCost, req.nav(), BigDecimal.ZERO);
            });

            OrderResult result = new OrderResult(orderId, "EXECUTED", req.amount(),
                    shares, req.nav(), fee, "模拟买入成功");
            orderHistory.add(result);
            return result;

        } else if ("SELL".equals(req.tradeType())) {
            PaperPosition pos = positions.get(req.fundCode());
            if (pos == null) {
                return new OrderResult(orderId, "FAILED", BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "无持仓");
            }

            BigDecimal sellShares = req.amount().divide(req.nav(), 4, RoundingMode.HALF_DOWN);
            if (sellShares.compareTo(pos.shares) > 0) {
                sellShares = pos.shares;
            }

            BigDecimal fee = req.amount().multiply(new BigDecimal("0.005"))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal netAmount = req.amount().subtract(fee);
            cash = cash.add(netAmount);

            BigDecimal remaining = pos.shares.subtract(sellShares);
            if (remaining.compareTo(new BigDecimal("0.0001")) < 0) {
                positions.remove(req.fundCode());
            } else {
                positions.put(req.fundCode(), new PaperPosition(
                        req.fundCode(), remaining, pos.avgCost, req.nav(), BigDecimal.ZERO));
            }

            OrderResult result = new OrderResult(orderId, "EXECUTED", req.amount(),
                    sellShares, req.nav(), fee, "模拟卖出成功");
            orderHistory.add(result);
            return result;
        }

        return new OrderResult(orderId, "FAILED", BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "未知交易类型");
    }

    @Override
    public boolean cancelOrder(String orderId) {
        return false; // 模拟交易即时成交，不支持撤单
    }

    @Override
    public OrderResult queryOrder(String orderId) {
        return orderHistory.stream()
                .filter(o -> o.orderId().equals(orderId)).findFirst().orElse(null);
    }

    @Override
    public List<Position> queryPositions() {
        return positions.values().stream().map(p -> new Position(
                p.fundCode, p.fundCode, p.shares, p.avgCost, p.currentPrice,
                p.shares.multiply(p.currentPrice), p.unrealizedPnl, BigDecimal.ZERO
        )).toList();
    }

    @Override
    public AccountInfo queryAccount() {
        BigDecimal marketValue = positions.values().stream()
                .map(p -> p.shares.multiply(p.currentPrice))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAssets = cash.add(marketValue);
        return new AccountInfo(totalAssets, cash, marketValue, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Override public boolean testConnection() { return true; }
    @Override public String getBrokerName() { return "PaperTrading"; }
    @Override public boolean isSimulated() { return true; }

    /** 更新所有持仓的当前价格 */
    public void updatePrices(Map<String, BigDecimal> prices) {
        for (Map.Entry<String, BigDecimal> e : prices.entrySet()) {
            PaperPosition pos = positions.get(e.getKey());
            if (pos != null) {
                BigDecimal newValue = pos.shares.multiply(e.getValue());
                BigDecimal pnl = newValue.subtract(pos.shares.multiply(pos.avgCost));
                positions.put(e.getKey(), new PaperPosition(
                        pos.fundCode, pos.shares, pos.avgCost, e.getValue(), pnl));
            }
        }
        prices.forEach((code, price) ->
                priceHistory.add(new PriceRecord(LocalDate.now(), code, price)));
    }

    public BigDecimal getCash() { return cash; }
    public Map<String, PaperPosition> getPositions() { return Collections.unmodifiableMap(positions); }

    // --- 内部类型 ---

    public record PaperPosition(String fundCode, BigDecimal shares,
                                 BigDecimal avgCost, BigDecimal currentPrice, BigDecimal unrealizedPnl) {}

    private record PriceRecord(LocalDate date, String fundCode, BigDecimal price) {}
}
