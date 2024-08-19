package antifraud.repository;


import antifraud.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByNumberAndDateAfter(String number, LocalDateTime date);

    List<Transaction> findAllByOrderByIdAsc();

    List<Transaction> findAllByNumberOrderByIdAsc(String number);
}
