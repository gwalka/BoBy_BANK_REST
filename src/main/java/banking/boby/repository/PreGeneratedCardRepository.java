package banking.boby.repository;

import banking.boby.entity.PreGeneratedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreGeneratedCardRepository extends JpaRepository<PreGeneratedCard, Long> {

    Optional<PreGeneratedCard> findTopByOrderByIdDesc();

    @Query(value = "SELECT * FROM pre_generated_card ORDER BY id ASC LIMIT :limit", nativeQuery = true)
    List<PreGeneratedCard> findEarliestCards(@Param("limit") int limit);

}
