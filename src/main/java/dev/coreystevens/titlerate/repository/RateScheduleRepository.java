package dev.coreystevens.titlerate.repository;

import dev.coreystevens.titlerate.model.PolicyType;
import dev.coreystevens.titlerate.model.RateSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateScheduleRepository extends JpaRepository<RateSchedule, Long> {

    List<RateSchedule> findByStateAndPolicyTypeOrderByTierStart(String state, PolicyType policyType);
}
