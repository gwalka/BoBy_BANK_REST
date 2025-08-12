package banking.boby.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_generation_lock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardGenerationLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_card_number", nullable = false, unique = true, length = 255)
    private String encryptedCardNumber;

}