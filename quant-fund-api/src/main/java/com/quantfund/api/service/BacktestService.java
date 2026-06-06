package com.quantfund.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantfund.common.entity.BacktestRun;
import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.NavHistory;
import com.quantfund.common.repository.*;
import com.quantfund.core.backtesting.BacktestEngine;
import com.quantfund.core.backtesting.BacktestResult;
import com.quantfund.core.backtesting.CommissionModel;
import com.quantfund.core.strategy.Strategy;
import com.quantfund.core.strategy.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BacktestService {

    private static final Logger log = LoggerFactory.getLogger(BacktestService.class);
    private static final ObjectMapper json = new ObjectMapper();

    @Autowired private BacktestRunRepository runRepo;
    @Autowired private BacktestResultRepository resultRepo;
    @Autowired private StrategyRepository strategyRepo;
    @Autowired private FundRepository fundRepo;
    @Autowired private NavHistoryRepository navRepo;

    private final StrategyRegistry registry = new StrategyRegistry();

    /** 创建回测请求 */
    @Transactional
    public BacktestRun createBacktest(String strategyName, String strategyType,
                                       Map<String, Object> params,
                                       List<String> fundCodes,
                                       LocalDate startDate, LocalDate endDate,
                                       BigDecimal initialCapital) {

        // 1. 保存策略定义
        Strategy coreStrategy = StrategyRegistry.create(strategyType);
        coreStrategy.setParameters(params);

        com.quantfund.common.entity.Strategy entity = new com.quantfund.common.entity.Strategy();
        entity.setName(strategyName);
        entity.setStrategyType(strategyType);
        try {
            entity.setParameters(json.writeValueAsString(params));
        } catch (JsonProcessingException e) {
            entity.setParameters("{}");
        }
        entity.setMaxHoldings((Integer) params.getOrDefault("topN", 5));
        entity.setTargetUniverse("ALL");
        entity = strategyRepo.save(entity);

        // 2. 创建回测运行记录
        BacktestRun run = new BacktestRun();
        run.setStrategyId(entity.getId());
        run.setRunName(strategyName + "-" + LocalDate.now());
        run.setStatus("PENDING");
        run.setStartDate(startDate);
        run.setEndDate(endDate);
        run.setInitialCapital(initialCapital);
        run.setCommissionModel("chinese_fund");
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>(params);
            snapshot.put("fundCodes", fundCodes);
            run.setParametersSnapshot(json.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) { /* ignore */ }
        run = runRepo.save(run);

        log.info("Backtest created: id={}, strategy={}, funds={}, period={}→{}",
                run.getId(), strategyName, fundCodes.size(), startDate, endDate);

        // 3. 异步执行
        executeBacktest(run.getId(), coreStrategy, fundCodes);

        return run;
    }

    /** 异步执行回测 */
    @Async("backtestExecutor")
    public void executeBacktest(Long runId, Strategy strategy, List<String> fundCodes) {
        BacktestRun run = runRepo.findById(runId).orElse(null);
        if (run == null) return;

        try {
            run.setStatus("RUNNING");
            run.setStartedAt(LocalDateTime.now());
            runRepo.save(run);
            log.info("Backtest {} starting...", runId);

            // 1. 加载数据
            Map<String, Strategy.FundData> fundDataMap = loadFundData(
                    fundCodes, run.getStartDate(), run.getEndDate());

            // 2. 选择费率模型
            CommissionModel commission = CommissionModel.publicFund();
            // 如果基金池全部是ETF，使用ETF费率
            boolean allEtf = fundDataMap.keySet().stream().allMatch(code -> {
                Fund f = fundRepo.findByFundCode(code).orElse(null);
                return f != null && Boolean.TRUE.equals(f.getIsEtf());
            });
            if (allEtf) commission = CommissionModel.etf();

            // 3. 运行回测引擎
            BacktestEngine engine = new BacktestEngine(commission);
            BacktestResult result = engine.run(strategy, fundDataMap,
                    run.getStartDate(), run.getEndDate(), run.getInitialCapital());

            // 4. 持久化结果
            saveBacktestResult(run, result);

            run.setStatus("COMPLETED");
            log.info("Backtest {} completed: return={}%",
                    runId, result.getTotalReturn().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));

        } catch (Exception e) {
            log.error("Backtest {} failed", runId, e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
        } finally {
            run.setCompletedAt(LocalDateTime.now());
            if (run.getStartedAt() != null) {
                run.setDurationSeconds(
                        (int) java.time.Duration.between(run.getStartedAt(), run.getCompletedAt()).toSeconds());
            }
            runRepo.save(run);
        }
    }

    /** 加载基金历史数据 */
    private Map<String, Strategy.FundData> loadFundData(
            List<String> fundCodes, LocalDate start, LocalDate end) {

        Map<String, Strategy.FundData> result = new LinkedHashMap<>();

        for (String code : fundCodes) {
            Fund fund = fundRepo.findByFundCode(code).orElse(null);
            if (fund == null) continue;

            List<NavHistory> navList = navRepo.findByFundIdAndNavDateBetweenOrderByNavDateAsc(
                    fund.getId(), start, end);
            if (navList.size() < 60) continue; // 至少需要3个月数据

            List<BigDecimal> navSeries = navList.stream().map(NavHistory::getUnitNav).toList();
            List<BigDecimal> returnSeries = navList.stream()
                    .map(n -> n.getDailyReturn() != null ? n.getDailyReturn() : BigDecimal.ZERO)
                    .toList();
            List<LocalDate> dates = navList.stream().map(NavHistory::getNavDate).toList();

            result.put(code, new Strategy.FundData(
                    code, fund.getFundName(), navSeries, returnSeries, dates,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            ));
        }

        return result;
    }

    /** 保存回测结果 */
    @Transactional
    protected void saveBacktestResult(BacktestRun run, BacktestResult result) {
        com.quantfund.common.entity.BacktestResult entity = new com.quantfund.common.entity.BacktestResult();
        entity.setBacktestId(run.getId());
        entity.setTotalReturn(result.getTotalReturn());
        entity.setAnnualReturn(result.getAnnualReturn());
        entity.setVolatility(result.getVolatility());
        entity.setSharpeRatio(result.getSharpeRatio());
        entity.setSortinoRatio(result.getSortinoRatio());
        entity.setCalmarRatio(result.getCalmarRatio());
        entity.setMaxDrawdown(result.getMaxDrawdown());
        entity.setMaxDrawdownStart(result.getMaxDrawdownStart());
        entity.setMaxDrawdownEnd(result.getMaxDrawdownEnd());
        entity.setWinRate(result.getWinRate());
        entity.setTotalTrades(result.getTotalTrades());

        // 序列化详细数据
        try {
            if (result.getEquityCurveDates() != null && result.getEquityCurveValues() != null) {
                List<List<Object>> equityCurve = new ArrayList<>();
                for (int i = 0; i < result.getEquityCurveDates().size(); i++) {
                    equityCurve.add(List.of(
                            result.getEquityCurveDates().get(i).toString(),
                            result.getEquityCurveValues().get(i).doubleValue()
                    ));
                }
                entity.setEquityCurve(json.writeValueAsString(equityCurve));
            }

            if (result.getTrades() != null) {
                entity.setTradeLog(json.writeValueAsString(result.getTrades().stream()
                        .map(t -> Map.of(
                                "date", t.date().toString(),
                                "fundCode", t.fundCode(),
                                "type", t.type(),
                                "shares", t.shares().doubleValue(),
                                "price", t.price().doubleValue(),
                                "amount", t.amount().doubleValue(),
                                "fee", t.fee().doubleValue(),
                                "reason", t.reason()
                        )).toList()));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize backtest details", e);
        }

        resultRepo.save(entity);
    }

    /** 获取回测状态 */
    public Optional<BacktestRun> getBacktestStatus(Long runId) {
        return runRepo.findById(runId);
    }

    /** 获取回测结果 */
    public Optional<com.quantfund.common.entity.BacktestResult> getBacktestResult(Long runId) {
        return resultRepo.findByBacktestId(runId);
    }

    /** 获取所有完成的回测 */
    public List<BacktestRun> listCompletedBacktests() {
        return runRepo.findByStatus("COMPLETED");
    }
}
