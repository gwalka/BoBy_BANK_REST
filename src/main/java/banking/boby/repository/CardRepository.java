package banking.boby.repository;

import banking.boby.entity.Card;
import banking.boby.entity.enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByExpiryDateBeforeAndStatusIn(LocalDate date, List<CardStatus> statuses);

    Page<Card> findByHolderId(Long userId, Pageable pageable);
}
