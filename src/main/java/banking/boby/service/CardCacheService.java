package banking.boby.service;

import banking.boby.entity.PreGeneratedCard;
import banking.boby.exception.CardGenerationException;
import banking.boby.repository.PreGeneratedCardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@Service
public class CardCacheService {

    @Value("${card.cache-size}")
    private int cacheSize;

    @Value("${card.generation-count}")
    private int generationCount;

    private final PreGeneratedCardRepository preGeneratedCardRepository;
    private final CardGeneratorService cardGeneratorService;
    private BlockingQueue<PreGeneratedCard> queue;
    private final ReentrantLock lock = new ReentrantLock();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CardCacheService(
            @Value("${card.cache-size}") int cacheSize,
            @Value("${card.generation-count}") int generationCount,
            PreGeneratedCardRepository preGeneratedCardRepository,
            CardGeneratorService cardGeneratorService
    ) {
        this.cacheSize = cacheSize;
        this.generationCount = generationCount;
        this.preGeneratedCardRepository = preGeneratedCardRepository;
        this.cardGeneratorService = cardGeneratorService;
        this.queue = new ArrayBlockingQueue<>(cacheSize);
    }

    public PreGeneratedCard takePreGeneratedCardNumber() {
        PreGeneratedCard card = queue.poll();
        if (card == null) {
            fillCache();

            card = queue.poll();
            if (card == null) {
                throw new CardGenerationException("Не удалось сгенерировать карты");
            }
        }

        checkCacheAsync();

        return card;
    }


    public void fillCache() {
        lock.lock();
        try {
            if (queue.isEmpty()) {
                if (preGeneratedCardRepository.count() < cacheSize) {
                    log.info("Репозиторий пуст, запускаю генератор");
                    cardGeneratorService.generateCards(generationCount);
                }
                loadCardsToCache(cacheSize);
                log.info("Карты загружены из репозитория");
            }
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public void loadCardsToCache(int count) {
        List<PreGeneratedCard> cards = preGeneratedCardRepository.findEarliestCards(count);
        preGeneratedCardRepository.deleteAllInBatch(cards);
        for (PreGeneratedCard card : cards) {
            queue.offer(card);
        }
    }

    private void checkCacheAsync() {
        if (queue.size() <= cacheSize * 0.3) {
            log.info("Кеш опустел, запускаю генерацию");
        }
            executor.submit(() -> {
                try {
                    fillCache();
                } catch (Exception e) {
                    log.error("Ошибка при асинхронном пополнении кеша", e);
                }
        });
    }

}
