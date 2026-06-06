package com.quantfund.common.repository;

import com.quantfund.common.entity.NavHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NavHistoryRepository extends JpaRepository<NavHistory, Long> {

    List<NavHistory> findByFundIdAndNavDateBetweenOrderByNavDateAsc(
            Long fundId, LocalDate start, LocalDate end);

    Optional<NavHistory> findTopByFundIdOrderByNavDateDesc(Long fundId);

    @Query("SELECT n FROM NavHistory n WHERE n.fundId = :fundId ORDER BY n.navDate DESC")
    List<NavHistory> findRecentByFundId(@Param("fundId") Long fundId);

    @Query(value = "SELECT n FROM NavHistory n WHERE n.fundId = :fundId AND n.navDate = " +
                   "(SELECT MAX(n2.navDate) FROM NavHistory n2 WHERE n2.fundId = :fundId)")
    Optional<NavHistory> findLatestByFundId(@Param("fundId") Long fundId);

    @Query("SELECT n.navDate FROM NavHistory n WHERE n.fundId = :fundId ORDER BY n.navDate ASC")
    List<LocalDate> findAllDatesByFundId(@Param("fundId") Long fundId);

    @Query("SELECT n.adjNav FROM NavHistory n WHERE n.fundId = :fundId " +
           "AND n.navDate BETWEEN :start AND :end ORDER BY n.navDate ASC")
    List<BigDecimal> findAdjNavSeries(@Param("fundId") Long fundId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    @Query("SELECT n.dailyReturn FROM NavHistory n WHERE n.fundId = :fundId " +
           "AND n.navDate BETWEEN :start AND :end AND n.dailyReturn IS NOT NULL " +
           "ORDER BY n.navDate ASC")
    List<BigDecimal> findDailyReturnSeries(@Param("fundId") Long fundId,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    @Query("SELECT COUNT(n) FROM NavHistory n WHERE n.fundId = :fundId")
    Long countByFundId(@Param("fundId") Long fundId);

    @Query("SELECT n FROM NavHistory n WHERE n.fundId IN :fundIds " +
           "AND n.navDate BETWEEN :start AND :end ORDER BY n.fundId, n.navDate ASC")
    List<NavHistory> findBulkNavData(@Param("fundIds") List<Long> fundIds,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    boolean existsByFundIdAndNavDate(Long fundId, LocalDate navDate);

    @Query("SELECT DISTINCT n.navDate FROM NavHistory n ORDER BY n.navDate DESC")
    List<LocalDate> findAllTradingDays();
}
