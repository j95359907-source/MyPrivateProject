package com.quantfund.core.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 量化策略接口 —— 所有策略必须实现此接口
 *
 * 策略模式：每个策略实现 generateSignals() 方法，
 * 接收历史数据，返回交易信号。
 */
public interface Strategy {

    /** 策略唯一名称 */
    String getName();

    /** 策略类型：MOMENTUM / MEAN_REVERSION / TREND / GRID / CUSTOM */
    String getType();

    /** 策略描述 */
    String getDescription();

    /**
     * 生成交易信号
     *
     * @param context 策略上下文（包含当前持仓、现金、日期等）
     * @param data    可用基金的历史数据（fundCode -> List of NAV records）
     * @return 交易信号列表
     */
    List<Signal> generateSignals(StrategyContext context, Map<String, FundData> data);

    /**
     * 获取策略参数
     */
    Map<String, Object> getParameters();

    /**
     * 更新策略参数
     */
    void setParameters(Map<String, Object> params);

    /**
     * 验证参数合法性
     */
    boolean validateParameters(Map<String, Object> params);

    // --- 内部类型 ---

    /** 交易信号 */
    record Signal(
            String fundCode,
            SignalType type,       // BUY / SELL / HOLD
            BigDecimal targetWeight, // 目标权重（0-1）
            BigDecimal confidence,   // 置信度（0-1）
            String reason            // 信号理由
    ) {}

    enum SignalType { BUY, SELL, HOLD }

    /** 策略上下文 */
    record StrategyContext(
            LocalDate currentDate,
            BigDecimal cashBalance,
            BigDecimal totalValue,
            Map<String, BigDecimal> currentWeights // fundCode -> weight
    ) {}

    /** 基金数据 */
    record FundData(
            String fundCode,
            String fundName,
            List<BigDecimal> navSeries,       // 复权净值序列
            List<BigDecimal> returnSeries,     // 日收益率序列
            List<LocalDate> dates,             // 对应日期
            BigDecimal return1m,
            BigDecimal return3m,
            BigDecimal return1y,
            BigDecimal volatility1y,
            BigDecimal sharpe1y
    ) {}
}
