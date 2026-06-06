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
    protected void syncNavForRecentFunds() {
        syncNavForFunds(30, 300, 80);
    }

    /**
     * 批量同步净值
     * @param daysBack 回看天数
     * @param maxFunds 最多同步基金数
     * @param delayMs 请求间隔毫秒
     */
    public void syncNavForFunds(int daysBack, int maxFunds, int delayMs) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(daysBack);

            // 筛选真实ETF（优先51xxxx沪市，再159xxx深市，最后LOF 16xxxx）
            // 51xxxx基金覆盖最全，159xxx中只有前几十只有数据
            List<Fund> realFunds = fundRepository.findAll().stream()
                    .filter(f -> {
                        String code = f.getFundCode();
                        if (code == null) return false;
                        return code.startsWith("51") || code.startsWith("159") || code.startsWith("16");
                    })
                    .sorted((a, b) -> {
                        // 51xxxx优先 → 159xxx次之 → 16xxxx最后
                        int pa = priority(a.getFundCode());
                        int pb = priority(b.getFundCode());
                        if (pa != pb) return Integer.compare(pa, pb);
                        return a.getFundCode().compareTo(b.getFundCode());
                    })
                    .limit(maxFunds)
                    .toList();

            log.info("开始净值同步: {}只基金, 区间{}→{}, 间隔{}ms",
                    realFunds.size(), startDate, endDate, delayMs);

            int syncCount = 0;
            int skippedCount = 0;
            long totalRecords = 0;

            for (int i = 0; i < realFunds.size(); i++) {
                Fund fund = realFunds.get(i);
                try {
                    // 检查是否已同步足够数据
                    Long existingCount = navHistoryRepository.countByFundId(fund.getId());
                    if (existingCount != null && existingCount > daysBack * 0.8) {
                        skippedCount++;
                        continue; // 已有足够数据
                    }

                    List<NavHistory> navList = collector.fetchNavHistory(
                            fund.getFundCode(), startDate, endDate);

                    // 按日期升序排列（API返回倒序）
                    if (navList != null && !navList.isEmpty()) {
                        navList.sort((a, b) -> a.getNavDate().compareTo(b.getNavDate()));
                    }

                    // 填充fundId
                    final Long fundId = fund.getId();
                    navList.forEach(n -> n.setFundId(fundId));

                    // 数据校验
                    ValidationResult validation = validator.validateNavData(navList);
                    if (validation.isValid() && !navList.isEmpty()) {
                        // 逐条保存（已存在的跳过）
                        int saved = 0;
                        for (NavHistory nav : navList) {
                            if (!navHistoryRepository.existsByFundIdAndNavDate(
                                    fund.getId(), nav.getNavDate())) {
                                navHistoryRepository.save(nav);
                                saved++;
                            }
                        }
                        if (saved > 0) {
                            syncCount++;
                            totalRecords += saved;
                            if (i % 20 == 0) {
                                log.info("进度: {}/{} 已同步{}只({}条), 跳过{}只",
                                        i + 1, realFunds.size(), syncCount, totalRecords, skippedCount);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("同步基金{}净值失败: {}", fund.getFundCode(), e.getMessage());
                }

                Thread.sleep(delayMs);
            }

            log.info("净值同步完成: 成功{}只, 跳过{}只, 总计{}条记录",
                    syncCount, skippedCount, totalRecords);
        } catch (Exception e) {
            log.error("净值同步失败", e);
        }
    }

    private static int priority(String code) {
        if (code.startsWith("51")) return 0;
        if (code.startsWith("159")) return 1;
        return 2;
    }
}
