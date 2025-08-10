package banking.boby.exception;

public class CardDecryptionException extends RuntimeException {

    public CardDecryptionException(String message, Object... args) {
        super(String.format(message, args));
    }
}

