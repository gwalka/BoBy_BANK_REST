package banking.boby.service;

import banking.boby.security.CardEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardEncryptorTest {

    private CardEncryptor cardEncryptor;

    private final String key = "1234567890123456";
    private final String iv = "abcdef9876543210";

    @BeforeEach
    void setUp() {
        cardEncryptor = new CardEncryptor();

        try {
            var keyField = CardEncryptor.class.getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(cardEncryptor, key);

            var ivField = CardEncryptor.class.getDeclaredField("iv");
            ivField.setAccessible(true);
            ivField.set(cardEncryptor, iv);
        } catch (Exception e) {
            fail("Не удалось установить ключ и iv для теста");
        }
    }

    @Test
    void testEncryptDecrypt() {
        String original = "1234567890123456";

        String encrypted = cardEncryptor.encrypt(original);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
        assertNotEquals(original, encrypted);

        String decrypted = cardEncryptor.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptWithInvalidDataThrows() {
        String invalidData = "invalidData";

        assertThrows(RuntimeException.class, () -> {
            cardEncryptor.decrypt(invalidData);
        });
    }
}

