package banking.boby.repository;

import banking.boby.entity.CardGenerationLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CardGenerationLockRepository extends JpaRepository<CardGenerationLock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CardGenerationLock> findTopByOrderByIdDesc();
}
