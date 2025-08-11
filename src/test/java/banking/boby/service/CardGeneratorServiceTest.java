package banking.boby.service;

import banking.boby.entity.PreGeneratedCard;
import banking.boby.repository.CardGenerationLockRepository;
import banking.boby.repository.PreGeneratedCardRepository;
import banking.boby.security.CardEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CardGeneratorServiceTest {

    private CardEncryptor cardEncryptor;
    private PreGeneratedCardRepository preGeneratedCardRepository;
    private CardGeneratorService cardGeneratorService;
    private CardGenerationLockRepository cardGenerationLockRepository;

    @BeforeEach
    void setUp() {
        cardEncryptor = mock(CardEncryptor.class);
        preGeneratedCardRepository = mock(PreGeneratedCardRepository.class);
        cardGenerationLockRepository = mock(CardGenerationLockRepository.class);
        cardGeneratorService = new CardGeneratorService(
                cardEncryptor,
                preGeneratedCardRepository,
                cardGenerationLockRepository);

        setField(cardGeneratorService, "bin", "123456");
        setField(cardGeneratorService, "generateCount", 3);
    }

    @Test
    void positiveGenerateCards() {
        int generateCount = 3;

        when(cardGenerationLockRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        when(cardEncryptor.encrypt(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0) + "-enc");

        when(preGeneratedCardRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<PreGeneratedCard> result = cardGeneratorService.generateCards(generateCount);

        assertNotNull(result);
        assertEquals(generateCount, result.size());

        when(cardGenerationLockRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        verify(cardEncryptor, times(generateCount + 1)).encrypt(anyString());
        verify(preGeneratedCardRepository).saveAll(anyList());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
