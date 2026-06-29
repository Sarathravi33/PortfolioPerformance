package com.portfolio.performance.repository;

import com.portfolio.performance.model.ValuationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValuationAuditRepository extends JpaRepository<ValuationAudit, Long> {
}
