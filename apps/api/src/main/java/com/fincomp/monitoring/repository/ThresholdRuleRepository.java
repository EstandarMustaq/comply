package com.fincomp.monitoring.repository;

import com.fincomp.monitoring.entity.ThresholdRule;
import com.fincomp.monitoring.entity.ThresholdRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThresholdRuleRepository extends JpaRepository<ThresholdRule, UUID> {
    List<ThresholdRule> findByActiveTrue();
    List<ThresholdRule> findByTypeAndActiveTrue(ThresholdRuleType type);
}
