package banking.boby.service;

import banking.boby.entity.PreGeneratedCard;
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

    @BeforeEach
    void setUp() {
        cardEncryptor = mock(CardEncryptor.class);
        preGeneratedCardRepository = mock(PreGeneratedCardRepository.class);
        cardGeneratorService = new CardGeneratorService(cardEncryptor, preGeneratedCardRepository);

        setField(cardGeneratorService, "bin", "123456");
        setField(cardGeneratorService, "generateCount", 3);
    }

    @Test
    void positiveGenerateCards() {
        int generateCount = 3;

        when(preGeneratedCardRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        when(cardEncryptor.encrypt(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0) + "-enc");

        when(preGeneratedCardRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<PreGeneratedCard> result = cardGeneratorService.generateCards(generateCount);

        assertNotNull(result);
        assertEquals(generateCount, result.size());

        verify(preGeneratedCardRepository).findTopByOrderByIdDesc();
        verify(cardEncryptor, times(generateCount)).encrypt(anyString());
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
