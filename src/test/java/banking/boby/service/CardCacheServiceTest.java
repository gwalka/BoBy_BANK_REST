package banking.boby.service;

import banking.boby.entity.PreGeneratedCard;
import banking.boby.repository.PreGeneratedCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardCacheServiceTest {

    private PreGeneratedCardRepository repository;
    private CardGeneratorService generatorService;
    private CardCacheService cardCacheService;

    private final int cacheSize = 5;
    private final int generationCount = 3;

    @BeforeEach
    void setUp() {
        repository = mock(PreGeneratedCardRepository.class);
        generatorService = mock(CardGeneratorService.class);
        cardCacheService = new CardCacheService(cacheSize, generationCount, repository, generatorService);
    }


    @Test
    void positiveGetCardFromCache() {
        PreGeneratedCard card = mock(PreGeneratedCard.class);
        BlockingQueue<PreGeneratedCard> queue = getQueueFromService(cardCacheService);
        queue.offer(card);

        PreGeneratedCard result = cardCacheService.takePreGeneratedCardNumber();

        assertEquals(card, result);
    }

    @Test
    void positiveFillCacheFromGenerate() {
        when(repository.count()).thenReturn(0L);
        PreGeneratedCard card = mock(PreGeneratedCard.class);
        when(repository.findEarliestCards(cacheSize)).thenReturn(List.of(card));

        BlockingQueue<PreGeneratedCard> queue = getQueueFromService(cardCacheService);
        assertTrue(queue.isEmpty());

        cardCacheService.fillCache();

        verify(generatorService).generateCards(generationCount);
        assertEquals(1, queue.size());
    }

    @Test
    void positiveLoadCardsToCache() {
        PreGeneratedCard card1 = mock(PreGeneratedCard.class);
        PreGeneratedCard card2 = mock(PreGeneratedCard.class);
        List<PreGeneratedCard> cards = List.of(card1, card2);

        when(repository.findEarliestCards(2)).thenReturn(cards);

        cardCacheService.loadCardsToCache(2);

        BlockingQueue<PreGeneratedCard> queue = getQueueFromService(cardCacheService);

        assertEquals(2, queue.size());
        assertTrue(queue.contains(card1));
        assertTrue(queue.contains(card2));

        verify(repository).deleteAllInBatch(cards);
    }

    @Test
    void positiveGetCardsFromRepo() {
        when(repository.count()).thenReturn((long) cacheSize + 1);
        PreGeneratedCard card = mock(PreGeneratedCard.class);
        when(repository.findEarliestCards(cacheSize)).thenReturn(List.of(card));

        cardCacheService.fillCache();

        verify(generatorService, never()).generateCards(anyInt());
    }


    @SuppressWarnings("unchecked")
    private BlockingQueue<PreGeneratedCard> getQueueFromService(CardCacheService service) {
        try {
            java.lang.reflect.Field queueField = CardCacheService.class.getDeclaredField("queue");
            queueField.setAccessible(true);
            return (BlockingQueue<PreGeneratedCard>) queueField.get(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
