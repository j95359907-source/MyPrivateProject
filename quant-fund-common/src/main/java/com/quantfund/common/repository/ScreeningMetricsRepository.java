package com.quantfund.common.repository;

import com.quantfund.common.entity.ScreeningMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningMetricsRepository extends JpaRepository<ScreeningMetrics, Long> {

    Optional<ScreeningMetrics> findByFundIdAndCalcDate(Long fundId, LocalDate calcDate);

    List<ScreeningMetrics> findByCalcDate(LocalDate calcDate);

    @Query("SELECT s FROM ScreeningMetrics s WHERE s.calcDate = :calcDate AND s.sharpe1y IS NOT NULL " +
           "ORDER BY s.sharpe1y DESC")
    List<ScreeningMetrics> findTopBySharpe(@Param("calcDate") LocalDate calcDate);

    @Query("SELECT s FROM ScreeningMetrics s WHERE s.calcDate = :calcDate AND s.return1y IS NOT NULL " +
           "ORDER BY s.return1y DESC")
    List<ScreeningMetrics> findTopByReturn1y(@Param("calcDate") LocalDate calcDate);

    @Query("SELECT s FROM ScreeningMetrics s WHERE s.calcDate = :calcDate " +
           "AND s.maxDrawdown1y IS NOT NULL ORDER BY s.maxDrawdown1y ASC")
    List<ScreeningMetrics> findTopByMinDrawdown(@Param("calcDate") LocalDate calcDate);

    @Query("SELECT MAX(s.calcDate) FROM ScreeningMetrics s")
    LocalDate findLatestCalcDate();
}
