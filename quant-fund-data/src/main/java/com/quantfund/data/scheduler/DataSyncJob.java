package com.quantfund.data.scheduler;

import com.quantfund.data.collector.DataCollector;
import com.quantfund.data.collector.EastMoneyCollector;
import com.quantfund.data.validator.DataValidator;
import com.quantfund.data.validator.ValidationResult;
import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.NavHistory;
import com.quantfund.common.repository.FundRepository;
import com.quantfund.common.repository.NavHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 数据同步定时任务 —— 每日自动从东方财富拉取最新数据
 *
 * 执行时间（默认）：
 * - 基金列表同步：每周六凌晨2:00
 * - 净值数据同步：每个交易日19:00和22:00（基金净值通常在18:00后陆续公布）
 */
@Component
public class DataSyncJob {

    private static final Logger log = LoggerFactory.getLogger(DataSyncJob.class);

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private NavHistoryRepository navHistoryRepository;

    private final DataCollector collector;
    private final DataValidator validator;

    public DataSyncJob() {
        this.collector = new EastMoneyCollector();
        this.validator = new DataValidator();
    }

    /**
     * 基金列表同步 —— 每周六2:00执行
     */
    @Scheduled(cron = "0 0 2 * * SAT")
    public void syncFundList() {
        log.info("=== 开始基金列表同步 ===");
        try {
            List<Fund> funds = collector.fetchFundList();
            int newCount = 0;
            int updateCount = 0;

            for (Fund fund : funds) {
                Optional<Fund> existing = fundRepository.findByFundCode(fund.getFundCode());
                if (existing.isPresent()) {
                    Fund dbFund = existing.get();
                    // 更新可能变化的信息
                    dbFund.setFundName(fund.getFundName());
                    dbFund.setFundShortName(fund.getFundShortName());
                    dbFund.setFundType(fund.getFundType());
                    dbFund.setFundSize(fund.getFundSize());
                    fundRepository.save(dbFund);
                    updateCount++;
                } else {
                    fundRepository.save(fund);
                    newCount++;
                }
            }

            log.info("基金列表同步完成: 新增{}只, 更新{}只, 总计{}只",
                    newCount, updateCount, fundRepository.count());
        } catch (Exception e) {
            log.error("基金列表同步失败", e);
        }
    }

    /**
     * 每日净值同步 —— 每个交易日19:00执行（第一次尝试）
     */
    @Scheduled(cron = "0 0 19 * * MON-FRI")
    public void syncNavDataEvening() {
        log.info("=== 晚间净值同步(19:00) ===");
        syncNavForRecentFunds();
    }

    /**
     * 每日净值同步 —— 每个交易日22:00执行（确保获取延迟更新的基金）
     */
    @Scheduled(cron = "0 0 22 * * MON-FRI")
    public void syncNavDataNight() {
        log.info("=== 夜间净值同步(22:00) ===");
        syncNavForRecentFunds();
    }

    /**
     * 为活跃基金同步最新净值（最近30天）
     */
    @Transactional
    protected void syncNavForRecentFunds() {
        try {
            List<Fund> activeFunds = fundRepository.findByFundTypeInAndIsActiveTrue(
                    List.of("ETF", "LOF", "开放式", "QDII", "FOF"));

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);

            int syncCount = 0;
            List<Fund> etfFunds = activeFunds.stream()
                    .filter(f -> Boolean.TRUE.equals(f.getIsEtf()))
                    .limit(200) // 先同步主要ETF（数量有限）
                    .toList();

            for (Fund fund : etfFunds) {
                try {
                    // 检查该基金最近是否已同步
                    Optional<NavHistory> latest = navHistoryRepository.findLatestByFundId(fund.getId());
                    if (latest.isPresent() &&
                            latest.get().getNavDate().isEqual(endDate) &&
                            !latest.get().getNavDate().isBefore(endDate.minusDays(1))) {
                        continue; // 今天已同步，跳过
                    }

                    List<NavHistory> navList = collector.fetchNavHistory(
                            fund.getFundCode(), startDate, endDate);

                    // 填充fundId
                    navList.forEach(n -> n.setFundId(fund.getId()));

                    // 数据校验
                    ValidationResult validation = validator.validateNavData(navList);
                    if (validation.isValid()) {
                        // 逐条保存（已存在的跳过）
                        for (NavHistory nav : navList) {
                            if (!navHistoryRepository.existsByFundIdAndNavDate(
                                    fund.getId(), nav.getNavDate())) {
                                navHistoryRepository.save(nav);
                            }
                        }
                        syncCount++;
                    } else {
                        log.warn("基金{}净值数据校验未通过: {}", fund.getFundCode(), validation);
                    }
                } catch (Exception e) {
                    log.error("同步基金{}净值失败: {}", fund.getFundCode(), e.getMessage());
                }

                // 请求间隔（避免被封IP）
                Thread.sleep(200);
            }

            log.info("净值同步完成: 更新{}只ETF基金", syncCount);
        } catch (Exception e) {
            log.error("净值同步失败", e);
        }
    }
}
