package com.quantfund.common.repository;

import com.quantfund.common.entity.BacktestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BacktestRunRepository extends JpaRepository<BacktestRun, Long> {
    List<BacktestRun> findByStatus(String status);
    List<BacktestRun> findByStrategyIdOrderByCreatedAtDesc(Long strategyId);
}
