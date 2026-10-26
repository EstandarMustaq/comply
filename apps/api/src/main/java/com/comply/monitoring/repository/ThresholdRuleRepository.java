package com.comply.monitoring.repository;

import com.comply.monitoring.entity.ThresholdRule;
import com.comply.monitoring.entity.ThresholdRuleType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThresholdRuleRepository extends JpaRepository<ThresholdRule, UUID> {
  List<ThresholdRule> findByActiveTrue();

  List<ThresholdRule> findByTypeAndActiveTrue(ThresholdRuleType type);
}
