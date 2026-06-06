package com.quantfund.common.repository;

import com.quantfund.common.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {
    Optional<Strategy> findByName(String name);
    List<Strategy> findByIsActiveTrue();
    List<Strategy> findByStrategyType(String type);
}
