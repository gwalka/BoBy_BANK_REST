package banking.boby.service;

import banking.boby.entity.PreGeneratedCard;
import banking.boby.exception.CardDecryptionException;
import banking.boby.exception.CardGenerationException;
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

            Optional<PreGeneratedCard> lastCardOpt = preGeneratedCardRepository.findTopByOrderByIdDesc();

            long lastSuffix;

            if (lastCardOpt.isEmpty()) {
                lastSuffix = START_SUFFIX;
            } else {
                try {
                    PreGeneratedCard lastCard = lastCardOpt.get();
                    String decrypted = cardEncryptor.decrypt(lastCard.getCardNumberEncrypted());
                    String suffixStr = decrypted.substring(bin.length());

                    lastSuffix = Long.parseLong(suffixStr);
                } catch (CardDecryptionException e) {
                    throw new CardDecryptionException("Ошибка при расшифровке последней карты", e);
                }
            }

            long currentSuffix = lastSuffix;

            for (int i = 0; i < generateCount; i++) {
                int step = 133 + random.nextInt(1027 - 133 + 1);
                currentSuffix += step;

                String cardNumber = bin + currentSuffix;

                if (cardNumber.length() > CARD_LENGTH) {
                    throw new CardGenerationException("BIN исчерпан, превышена максимальная длина карты");
                }

                String suffixStr = String.format("%0" + (CARD_LENGTH - bin.length()) + "d", currentSuffix);
                cardNumber = bin + suffixStr;

                String encryptedCardNumber = cardEncryptor.encrypt(cardNumber);

                PreGeneratedCard card = PreGeneratedCard.builder().cardNumberEncrypted(encryptedCardNumber).build();

                cards.add(card);
            }

            return preGeneratedCardRepository.saveAll(cards);
        } finally {
            generationLock.unlock();
        }
    }
}
