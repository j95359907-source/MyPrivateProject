package com.quantfund.core.strategy;

import com.quantfund.core.strategy.impl.MeanReversionStrategy;
import com.quantfund.core.strategy.impl.MomentumStrategy;
import com.quantfund.core.strategy.impl.MovingAverageCrossStrategy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略注册表 —— 管理所有可用策略实例
 *
 * 支持：
 * - 内置策略自动注册
 * - 运行时动态创建/销毁策略实例
 * - 按名称查找策略
 */
public class StrategyRegistry {

    /** 策略类型→工厂（用于创建新实例） */
    private static final Map<String, StrategyFactory> factories = new LinkedHashMap<>();

    /** 策略名称→实例（运行中的策略） */
    private final Map<String, Strategy> instances = new ConcurrentHashMap<>();

    static {
        // 注册内置策略类型
        registerType("MOMENTUM", MomentumStrategy::new, "动量轮动策略",
                "买入过去N个月表现最强的基金，定期轮换");
        registerType("MEAN_REVERSION", MeanReversionStrategy::new, "均值回归策略",
                "买入超跌基金，等待回归均线后卖出");
        registerType("TREND", MovingAverageCrossStrategy::new, "双均线策略",
                "快线上穿慢线买入，下穿卖出");
    }

    /** 注册策略类型 */
    public static void registerType(String type, StrategyFactory factory,
                                     String name, String description) {
        factories.put(type, factory);
    }

    /** 创建策略实例 */
    public static Strategy create(String type) {
        StrategyFactory factory = factories.get(type);
        if (factory == null) throw new IllegalArgumentException("未知策略类型: " + type);
        return factory.create();
    }

    /** 获取所有可用策略类型 */
    public static List<StrategyTypeInfo> listTypes() {
        List<StrategyTypeInfo> types = new ArrayList<>();
        for (Map.Entry<String, StrategyFactory> entry : factories.entrySet()) {
            Strategy s = entry.getValue().create();
            types.add(new StrategyTypeInfo(entry.getKey(), s.getName(), s.getDescription(), s.getParameters()));
        }
        return types;
    }

    /** 注册运行中的策略实例 */
    public void registerInstance(String name, Strategy strategy) {
        instances.put(name, strategy);
    }

    /** 获取运行中的策略 */
    public Optional<Strategy> getInstance(String name) {
        return Optional.ofNullable(instances.get(name));
    }

    /** 移除策略实例 */
    public void removeInstance(String name) {
        instances.remove(name);
    }

    public Map<String, Strategy> getAllInstances() {
        return Collections.unmodifiableMap(instances);
    }

    // --- 内部类型 ---

    @FunctionalInterface
    public interface StrategyFactory {
        Strategy create();
    }

    public record StrategyTypeInfo(String type, String name, String description, Map<String, Object> defaultParams) {}
}
