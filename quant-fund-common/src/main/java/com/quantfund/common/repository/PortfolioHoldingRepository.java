package com.quantfund.common.repository;

import com.quantfund.common.entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {
    List<PortfolioHolding> findByPortfolioIdAndIsActiveTrue(Long portfolioId);
    List<PortfolioHolding> findByPortfolioId(Long portfolioId);
}
