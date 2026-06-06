package com.quantfund.common.repository;

import com.quantfund.common.entity.BacktestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {
    Optional<BacktestResult> findByBacktestId(Long backtestId);
    void deleteByBacktestId(Long backtestId);
}
