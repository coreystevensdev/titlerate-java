package dev.coreystevens.titlerate.repository;

import dev.coreystevens.titlerate.model.PolicyType;
import dev.coreystevens.titlerate.model.RateSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RateScheduleRepository extends JpaRepository<RateSchedule, Long> {

    List<RateSchedule> findByStateAndPolicyTypeOrderByTierStart(String state, PolicyType policyType);

    @Query("""
        SELECT r FROM RateSchedule r
        WHERE r.state = :state
          AND r.policyType = :policyType
          AND r.tierStart <= :amount
          AND (r.tierEnd IS NULL OR r.tierEnd > :amount)
        ORDER BY r.effectiveDate DESC
        """)
    Optional<RateSchedule> findApplicableSchedule(
        @Param("state") String state,
        @Param("policyType") PolicyType policyType,
        @Param("amount") BigDecimal amount
    );
}
