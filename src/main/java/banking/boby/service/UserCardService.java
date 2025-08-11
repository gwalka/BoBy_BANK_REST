package banking.boby.service;

import banking.boby.context.UserContext;
import banking.boby.dto.BalanceResponseDto;
import banking.boby.dto.CardDigitsDto;
import banking.boby.dto.CardUserDto;
import banking.boby.dto.TransferRequestDto;
import banking.boby.entity.Card;
import banking.boby.entity.Transaction;
import banking.boby.entity.enums.CardStatus;
import banking.boby.entity.enums.OperationType;
import banking.boby.exception.AccessDeniedException;
import banking.boby.exception.EntityNotFoundException;
import banking.boby.exception.UnsafeOperationException;
import banking.boby.exception.WrongCardOperationException;
import banking.boby.repository.CardRepository;
import banking.boby.repository.TransactionRepository;
import banking.boby.security.CardEncryptor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCardService {

    private static final Logger log = LoggerFactory.getLogger(UserCardService.class);
    private final CardRepository cardRepository;
    private final CardEncryptor cardEncryptor;
    private final TransactionRepository transactionRepository;


    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 5000, multiplier = 2),
            exclude = {AccessDeniedException.class, UnsafeOperationException.class})
    @Transactional(noRollbackFor = {AccessDeniedException.class, UnsafeOperationException.class})
    public void transferFunds(TransferRequestDto request) {
        List<Card> cards = validateTransfer(request);
        Card fromCard = cards.get(0);
        Card toCard = cards.get(1);

        BigDecimal newBalanceForFromCard = fromCard.getBalance().subtract(request.amount());
        Transaction sentFunds = Transaction.builder()
                .amount(request.amount().negate())
                .operationDateTime(LocalDateTime.now())
                .card(fromCard)
                .operationType(OperationType.TRANSFER)
                .build();
        fromCard.setBalance(newBalanceForFromCard);
        cardRepository.save(fromCard);
        transactionRepository.save(sentFunds);


        BigDecimal newBalanceForToCard = toCard.getBalance().add(request.amount());
        Transaction receiveFunds = Transaction.builder()
                .amount(request.amount())
                .operationDateTime(LocalDateTime.now())
                .card(toCard)
                .operationType(OperationType.TRANSFER)
                .build();
        toCard.setBalance(newBalanceForToCard);
        cardRepository.save(toCard);
        transactionRepository.save(receiveFunds);


        log.info("Перевод {} выполнен с карты {} на карту {} пользователем {}",
                request.amount(), fromCard.getId(), toCard.getId(), UserContext.getCurrentUserId());
    }

    public Page<CardUserDto> getMyCards(Pageable pageable, String search) {
        Long userId = UserContext.getCurrentUserId();
        assert userId != null;

        if (search == null || search.isBlank()) {
            return cardRepository.findByHolderId(userId, pageable)
                    .map(this::mapToCardUserDto);
        }
        String digitsSearch = search.replaceAll("\\D", "");
        if (digitsSearch.isEmpty()) {
            return cardRepository.findByHolderId(userId, pageable)
                    .map(this::mapToCardUserDto);
        }
        List<Card> allCards = cardRepository.findByHolderId(userId);
        List<CardUserDto> filtered = allCards.stream()
                .filter(card -> {
                    String decrypted = cardEncryptor.decrypt(card.getEncryptedNumber());
                    return decrypted.contains(digitsSearch);
                })
                .map(this::mapToCardUserDto)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<CardUserDto> pageContent = (start <= end) ? filtered.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    public BalanceResponseDto getBalance(Long cardId) {
        Long userId = UserContext.getCurrentUserId();
        Card card = validateCard(cardId);

        if (card.getHolder() == null || !card.getHolder().getId().equals(userId)) {
            throw new AccessDeniedException("Карта %d не принадлежит пользователю %d", cardId, userId);

        }

        return BalanceResponseDto.builder()
                .balance(card.getBalance())
                .cardId(cardId)
                .maskedNum(maskCardNumber(cardEncryptor.decrypt(card.getEncryptedNumber())))
                .build();
    }

    @Transactional
    public void blockCardRequest(Long cardId) {
        Long userId = UserContext.getCurrentUserId();
        Card card = validateCard(cardId);

        if (card.getHolder() == null || !card.getHolder().getId().equals(userId)) {
            throw new AccessDeniedException("Карта %d не  принадлежит пользователю %d", cardId, userId);
        }

        if (card.getStatus().equals(CardStatus.BLOCKED)) {
            throw new WrongCardOperationException("Карта уже заблокирована");
        }

        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        log.info("Запрос на блокировку карты {} выполнен пользователем {}", cardId, userId);

    }

    public CardDigitsDto getFullCardNumber(Long cardId) {
        Long userId = UserContext.getCurrentUserId();
        Card card = validateCard(cardId);

        if (card.getHolder() == null || !card.getHolder().getId().equals(userId)) {
            throw new AccessDeniedException("Карта %d не  принадлежит пользователю %d", cardId, userId);
        }

        String decryptedNum = cardEncryptor.decrypt(card.getEncryptedNumber());

        return CardDigitsDto.builder()
                .id(card.getId())
                .fullNumber(decryptedNum)
                .build();
    }

    private Card validateCard(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта с id %d не найдена", cardId));

    }

    private List<Card> validateTransfer(TransferRequestDto request) {
        Long userId = UserContext.getCurrentUserId();
        Card fromCard = validateCard(request.fromCardId());
        Card toCard = validateCard(request.toCardId());

        if (!fromCard.getHolder().getId().equals(userId) || !toCard.getHolder().getId().equals(userId)) {
            throw new UnsafeOperationException("Средства возможно переводить только между своими картами");
        }

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new UnsafeOperationException("Недостаточно средств для перевода");
        }

        if (request.amount().compareTo(BigDecimal.ONE) < 0) {
            throw new UnsafeOperationException("Сумма должна быть не меньше 1");
        }

        if (!fromCard.getStatus().equals(CardStatus.ACTIVE) || !toCard.getStatus().equals(CardStatus.ACTIVE)) {
            throw new UnsafeOperationException("Выберите активную карту");
        }


        if (fromCard.getId().equals(toCard.getId())) {
            throw new UnsafeOperationException("Выберите другую карту для получения");
        }

        return List.of(fromCard, toCard);
    }

    private String maskCardNumber(String decryptedNumber) {
        char[] chars = decryptedNumber.toCharArray();
        for (int i = 0; i < chars.length - 4; i++) {
            chars[i] = '*';
        }
        return new String(chars);
    }

    private CardUserDto mapToCardUserDto(Card card) {
        String decrypted = cardEncryptor.decrypt(card.getEncryptedNumber());
        String masked = maskCardNumber(decrypted);

        return CardUserDto.builder()
                .id(card.getId())
                .maskedNum(masked)
                .status(card.getStatus())
                .expDate(card.getExpiryDate())
                .balance(card.getBalance())
                .build();
    }


}


