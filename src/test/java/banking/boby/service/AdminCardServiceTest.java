package banking.boby.service;

import banking.boby.dto.CardAdminDto;
import banking.boby.entity.Card;
import banking.boby.entity.PreGeneratedCard;
import banking.boby.entity.User;
import banking.boby.entity.enums.CardStatus;
import banking.boby.exception.DataValidationException;
import banking.boby.exception.EntityNotFoundException;
import banking.boby.exception.WrongCardOperationException;
import banking.boby.repository.CardRepository;
import banking.boby.repository.UserRepository;
import banking.boby.security.CardEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCardServiceTest {

    private CardCacheService cardCacheService;
    private CardRepository cardRepository;
    private UserRepository userRepository;
    private CardEncryptor cardEncryptor;
    private AdminCardService adminCardService;

    @BeforeEach
    void setUp() {
        cardCacheService = mock(CardCacheService.class);
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        cardEncryptor = mock(CardEncryptor.class);
        adminCardService = new AdminCardService(cardCacheService, cardRepository, userRepository, cardEncryptor);
    }

    @Test
    void positiveCreateCard() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .build();

        PreGeneratedCard preCard = PreGeneratedCard.builder()
                .cardNumberEncrypted("enc-num")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardCacheService.takePreGeneratedCardNumber()).thenReturn(preCard);

        adminCardService.createCard(userId);

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void negativeCreateCardIdIsNull() {
        assertThrows(DataValidationException.class, () -> adminCardService.createCard(null));
        verifyNoInteractions(userRepository, cardCacheService, cardRepository);
    }

    @Test
    void negativeCreateCardUserNotExists() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> adminCardService.createCard(99L));
    }

    @Test
    void positiveBlockCard() {
        Card card = Card.builder().id(1L).status(CardStatus.ACTIVE).build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        adminCardService.blockCard(1L);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void postitiveActivateCard() {
        Card card = Card.builder().id(1L).status(CardStatus.BLOCKED).build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        adminCardService.activateCard(1L);

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void negativeActivateCard() {
        Card card = Card.builder().id(2L).status(CardStatus.ACTIVE).build();
        when(cardRepository.findById(2L)).thenReturn(Optional.of(card));

        assertThrows(WrongCardOperationException.class, () -> adminCardService.activateCard(2L));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void positiveDeleteCard() {
        Card card = Card.builder().id(1L).build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        adminCardService.deleteCard(1L);

        verify(cardRepository).delete(card);
    }

    @Test
    void negativeBlockCard() {
        Card card = Card.builder().id(1L).status(CardStatus.BLOCKED).build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(WrongCardOperationException.class, () -> adminCardService.blockCard(1L));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void positiveDeactivateExpiredCards() {
        Card card1 = Card.builder().id(1L).expiryDate(LocalDate.now().minusDays(1)).status(CardStatus.ACTIVE).build();
        Card card2 = Card.builder().id(2L).expiryDate(LocalDate.now().minusDays(1)).status(CardStatus.BLOCKED).build();

        when(cardRepository.findByExpiryDateBeforeAndStatusIn(any(LocalDate.class), anyList()))
                .thenReturn(List.of(card1, card2));

        adminCardService.deactivateExpiredCards();

        assertEquals(CardStatus.EXPIRED, card1.getStatus());
        assertEquals(CardStatus.EXPIRED, card2.getStatus());
        verify(cardRepository).saveAll(List.of(card1, card2));
    }

    @Test
    void positiveGetAllCards() {
        Card card = Card.builder()
                .id(1L)
                .expiryDate(LocalDate.now())
                .encryptedNumber("enc")
                .status(CardStatus.ACTIVE)
                .holder(User.builder().id(10L).build())
                .build();

        when(cardRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(card)));
        when(cardEncryptor.decrypt("enc")).thenReturn("1234567890123456");

        Page<CardAdminDto> result = adminCardService.getAllCards(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).maskedNum().endsWith("3456"));
    }
}
