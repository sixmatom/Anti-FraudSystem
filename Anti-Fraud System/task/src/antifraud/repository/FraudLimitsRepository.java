package antifraud.repository;

import antifraud.entity.FraudLimits;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudLimitsRepository extends JpaRepository<FraudLimits, Long> {
}