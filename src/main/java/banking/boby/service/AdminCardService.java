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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCardService {

    private final CardCacheService cardCacheService;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptor cardEncryptor;

    @Transactional
    public void createCard(Long userId) {
        if (userId == null) {
            throw new DataValidationException("Id не может быть пустым, введите значение");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id %d не найден", userId));

        PreGeneratedCard preGeneratedCard = cardCacheService.takePreGeneratedCardNumber();
        String cardEncryptedNum = preGeneratedCard.getCardNumberEncrypted();

        Card card = Card.builder()
                .encryptedNumber(cardEncryptedNum)
                .holder(user)
                .holderName(String.format("%s %s", user.getFirstName(), user.getLastName()))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .expiryDate(calculateExpiryDate())
                .build();

        cardRepository.save(card);
        log.info("Карта для пользователя {} создана", userId);
    }

    @Transactional
    public void blockCard(Long cardId) {
        Card card = validateCard(cardId);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new WrongCardOperationException("Карта с id %d уже заблокирована", cardId);
        }

        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        log.info("Карта {} успешно заблокирована", cardId);
    }

    @Transactional
    public void activateCard(Long cardId) {
        Card card = validateCard(cardId);
        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new WrongCardOperationException("Карта с id %d уже активирована", cardId);
        }

        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
        log.info("Карта {} успешно активирована", cardId);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = validateCard(cardId);
        cardRepository.delete(card);
        log.info("Карта {} успешно удалена", cardId);
    }

    @Transactional
    public void deactivateExpiredCards() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Moscow"));
        List<CardStatus> statuses = List.of(CardStatus.ACTIVE, CardStatus.BLOCKED);
        List<Card> expiredCards = cardRepository.findByExpiryDateBeforeAndStatusIn(today, statuses);

        expiredCards.forEach(card -> card.setStatus(CardStatus.EXPIRED));

        cardRepository.saveAll(expiredCards);
        log.info("Заблокировано {} карт с истекшим сроком действия", expiredCards.size());
    }

    public Page<CardAdminDto> getAllCards(Pageable pageable) {
        Page<Card> pageCards = cardRepository.findAll(pageable);

        return pageCards.map(card -> {
            String decrypted = cardEncryptor.decrypt(card.getEncryptedNumber());
            String masked = maskCardNumber(decrypted);

            return CardAdminDto.builder()
                    .id(card.getId())
                    .expDate(card.getExpiryDate())
                    .maskedNum(masked)
                    .status(card.getStatus())
                    .userId(card.getHolder().getId())
                    .build();


        });
    }

    private Card validateCard(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Карта с id %d не существует", cardId));
    }


    private LocalDate calculateExpiryDate() {
        YearMonth now = YearMonth.now();
        YearMonth expiry = now.plusYears(3);
        return expiry.atEndOfMonth();
    }

    private String maskCardNumber(String decryptedNumber) {
        char[] chars = decryptedNumber.toCharArray();
        for (int i = 0; i < chars.length - 4; i++) {
            chars[i] = '*';
        }
        return new String(chars);
    }

}
