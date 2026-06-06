package com.quantfund.common.repository;

import com.quantfund.common.entity.Fund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundRepository extends JpaRepository<Fund, Long> {

    Optional<Fund> findByFundCode(String fundCode);

    List<Fund> findByFundType(String fundType);

    List<Fund> findByIsEtfTrue();

    List<Fund> findByIsIndexTrue();

    List<Fund> findByManagementCompany(String company);

    @Query("SELECT f FROM Fund f WHERE f.isActive = true ORDER BY f.fundSize DESC")
    Page<Fund> findActiveFunds(Pageable pageable);

    @Query("SELECT f FROM Fund f WHERE f.fundType = :fundType AND f.isActive = true")
    Page<Fund> findByFundTypeAndActive(@Param("fundType") String fundType, Pageable pageable);

    @Query("SELECT f FROM Fund f WHERE f.isActive = true AND " +
           "(:keyword IS NULL OR f.fundName LIKE %:keyword% OR f.fundCode LIKE %:keyword%)")
    Page<Fund> searchFunds(@Param("keyword") String keyword, Pageable pageable);

    List<Fund> findByFundTypeInAndIsActiveTrue(List<String> fundTypes);

    @Query("SELECT DISTINCT f.managementCompany FROM Fund f WHERE f.managementCompany IS NOT NULL ORDER BY f.managementCompany")
    List<String> findDistinctCompanies();

    @Query("SELECT DISTINCT f.fundType FROM Fund f ORDER BY f.fundType")
    List<String> findDistinctTypes();

    @Query("SELECT COUNT(f) FROM Fund f WHERE f.isActive = true")
    Long countActiveFunds();
}
