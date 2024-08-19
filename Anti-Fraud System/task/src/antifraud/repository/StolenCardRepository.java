package antifraud.repository;

import antifraud.entity.StolenCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StolenCardRepository extends JpaRepository<StolenCard, Long> {
    Optional<StolenCard> findByNumber(String number);

    void deleteByNumber(String number);

    List<StolenCard> findAllByOrderByIdAsc();

    boolean existsByNumber(String number);

}
