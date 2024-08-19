package antifraud.repository;

import antifraud.entity.SuspiciousIP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuspiciousIPRepository extends JpaRepository<SuspiciousIP, Long> {
    Optional<SuspiciousIP> findByIp(String ip);

    void deleteByIp(String ip);

    List<SuspiciousIP> findAllByOrderByIdAsc();

    boolean existsByIp(String ip);


}