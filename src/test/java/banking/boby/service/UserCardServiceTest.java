package banking.boby.service;

import banking.boby.context.UserContext;
import banking.boby.dto.BalanceResponseDto;
import banking.boby.dto.CardDigitsDto;
import banking.boby.dto.CardUserDto;
import banking.boby.dto.TransferRequestDto;
import banking.boby.entity.Card;
import banking.boby.entity.Transaction;
import banking.boby.entity.User;
import banking.boby.entity.enums.CardStatus;
import banking.boby.exception.AccessDeniedException;
import banking.boby.exception.UnsafeOperationException;
import banking.boby.repository.CardRepository;
import banking.boby.repository.TransactionRepository;
import banking.boby.security.CardEncryptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCardServiceTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    CardEncryptor cardEncryptor;

    private static MockedStatic<UserContext> mockedUserContext;

    @InjectMocks
    UserCardService userCardService;

    private final Long userId = 100L;

    @BeforeAll
    static void beforeAll() {
        mockedUserContext = Mockito.mockStatic(UserContext.class);
    }

    @AfterAll
    static void afterAll() {
        mockedUserContext.close();
    }

    @BeforeEach
    void setup() {
        mockedUserContext.reset();
        mockedUserContext.when(UserContext::getCurrentUserId).thenReturn(userId);
    }

    @Test
    void positieTransferFunds() {
        Long fromCardId = 1L;
        Long toCardId = 2L;
        BigDecimal amount = BigDecimal.valueOf(50);

        Card fromCard = createCard(fromCardId, userId, BigDecimal.valueOf(100));
        Card toCard = createCard(toCardId, userId, BigDecimal.valueOf(0));

        TransferRequestDto request = new TransferRequestDto(fromCardId, toCardId, amount);

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userCardService.transferFunds(request);

        assertEquals(BigDecimal.valueOf(50), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(50), toCard.getBalance());

        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void negativeTransferNotEnoughFunds() {
        Long fromCardId = 1L;
        Long toCardId = 2L;
        BigDecimal amount = BigDecimal.valueOf(150);

        Card fromCard = createCard(fromCardId, userId, BigDecimal.valueOf(100));
        Card toCard = createCard(toCardId, userId, BigDecimal.valueOf(0));

        TransferRequestDto request = new TransferRequestDto(fromCardId, toCardId, amount);

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));

        UnsafeOperationException ex = assertThrows(UnsafeOperationException.class, () -> userCardService.transferFunds(request));
        assertTrue(ex.getMessage().contains("Недостаточно средств"));
    }

    @Test
    void positiveGetBalance() {
        Card card = Card.builder()
                .id(1L)
                .holder(User.builder()
                        .id(100L)
                        .build())
                .balance(BigDecimal.valueOf(123.45))
                .encryptedNumber("encrypted")
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardEncryptor.decrypt("encrypted")).thenReturn("1234567890123456");

        BalanceResponseDto balance = userCardService.getBalance(1L);

        assertEquals(1L, balance.cardId());
        assertEquals(BigDecimal.valueOf(123.45), balance.balance());
        assertTrue(balance.maskedNum().startsWith("************"));
    }

    @Test
    void negativeGetBalanceAccessDenied() {
        Long cardId = 1L;
        Card card = createCard(cardId, 999L, BigDecimal.valueOf(50));

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(AccessDeniedException.class, () -> userCardService.getBalance(cardId));
    }

    @Test
    void positiveBlockCard() {
        Long cardId = 1L;
        Card card = createCard(cardId, userId, BigDecimal.ZERO);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userCardService.blockCardRequest(cardId);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
    }

    @Test
    void negativeBlockCardAccessDenied() {
        Long cardId = 1L;
        Card card = createCard(cardId, 999L, BigDecimal.ZERO);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(AccessDeniedException.class, () -> userCardService.blockCardRequest(cardId));
    }

    @Test
    void positiveGetCards() {
        Card card = Card.builder()
                .id(1L)
                .balance(BigDecimal.valueOf(100))
                .encryptedNumber("encrypted")
                .build();


        Page<Card> page = new PageImpl<>(List.of(card));
        when(cardRepository.findByHolderId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(cardEncryptor.decrypt("encrypted")).thenReturn("1234567890123456");

        Page<CardUserDto> result = userCardService.getMyCards(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("************3456", result.getContent().get(0).maskedNum());
    }

    @Test
    void positiveGetFullCardNumber() {
        String fullCardNum = "1234567812345678";
        User user = User.builder()
                .id(100L)
                .build();

        Card card = Card.builder()
                .holder(user)
                .id(1L)
                .encryptedNumber("encrypted")
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardEncryptor.decrypt("encrypted")).thenReturn(fullCardNum);

        CardDigitsDto result = userCardService.getFullCardNumber(card.getId());


        assertEquals(card.getId(), result.id());
        assertEquals(fullCardNum, result.fullNumber());
    }


    private Card createCard(Long id, Long holderId, BigDecimal balance) {
        banking.boby.entity.User user = banking.boby.entity.User.builder()
                .id(holderId)
                .build();

        return Card.builder()
                .id(id)
                .balance(balance)
                .status(CardStatus.ACTIVE)
                .holder(user)
                .expiryDate(LocalDate.now().plusYears(2))
                .build();
    }
}
