package banking.boby.service;

import banking.boby.entity.CardGenerationLock;
import banking.boby.entity.PreGeneratedCard;
import banking.boby.exception.CardDecryptionException;
import banking.boby.exception.CardGenerationException;
import banking.boby.repository.CardGenerationLockRepository;
import banking.boby.repository.PreGeneratedCardRepository;
import banking.boby.security.CardEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardGeneratorService {

    @Value("${card.bin}")
    private String bin;

    @Value("${card.generation-count}")
    private int generateCount;

    private final CardEncryptor cardEncryptor;
    private final PreGeneratedCardRepository preGeneratedCardRepository;
    private final CardGenerationLockRepository cardGenerationLockRepository;

    private static final int CARD_LENGTH = 16;
    private static final long START_SUFFIX = 1010212487L;
    private final Random random = new Random();
    private final ReentrantLock generationLock = new ReentrantLock();


    @Transactional(propagation = Propagation.REQUIRES_NEW)
        public List<PreGeneratedCard> generateCards(int generateCount) {
        if (!generationLock.tryLock()) {
            log.info("Генерация уже запущена");
            return Collections.emptyList();
        }

        try {
            List<PreGeneratedCard> cards = new ArrayList<>();

            Optional<CardGenerationLock> maxSuffix = cardGenerationLockRepository.findTopByOrderByIdDesc();

            long lastSuffix;

            if (maxSuffix.isEmpty()) {
                lastSuffix = START_SUFFIX;
            } else {
                try {
                    String decrypted = cardEncryptor.decrypt(maxSuffix.get().getEncryptedCardNumber());
                    lastSuffix = Long.parseLong(decrypted);
                } catch (CardDecryptionException e) {
                    throw new CardDecryptionException("Ошибка при расшифровке последней карты", e);
                }
            }

            long currentSuffix = lastSuffix;
            int attempts = 0;
            while (cards.size() < generateCount) {
                attempts++;
                int step = 133 + random.nextInt(1027 - 133 + 1);
                currentSuffix += step;

                String suffixStr = String.format("%0" + (CARD_LENGTH - bin.length()) + "d", currentSuffix);
                String cardNumber = bin + suffixStr;

                if (cardNumber.length() > CARD_LENGTH) {
                    throw new CardGenerationException("BIN исчерпан, превышена максимальная длина карты");
                }

                if (!validByLuhn(cardNumber)) {
                    continue;
                }

                String encryptedCardNumber = cardEncryptor.encrypt(cardNumber);

                PreGeneratedCard card = PreGeneratedCard.builder().cardNumberEncrypted(encryptedCardNumber).build();

                cards.add(card);
            }
            log.info("Сгенерировано {} карт за {} попыток", cards.size(), attempts);

            String encryptedSuffix = cardEncryptor.encrypt(String.valueOf(currentSuffix));
            CardGenerationLock newMaxSuffix = CardGenerationLock.builder()
                    .encryptedCardNumber(encryptedSuffix)
                    .build();
            cardGenerationLockRepository.save(newMaxSuffix);
            return preGeneratedCardRepository.saveAll(cards);
        } finally {
            generationLock.unlock();
        }
    }

    private boolean validByLuhn(String num) {
        int[] digits = num.chars()
                .map(c -> c - '0')
                .toArray();

        int sum = 0;
        boolean secondDigit = false;

        for (int i = digits.length - 1; i >= 0; i--) {
            int digit = digits[i];
            if (secondDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            secondDigit = !secondDigit;
        }

        return sum % 10 == 0;
    }
}
