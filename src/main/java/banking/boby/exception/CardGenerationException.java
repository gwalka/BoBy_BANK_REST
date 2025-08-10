package banking.boby.exception;

public class CardGenerationException extends RuntimeException {

    public CardGenerationException(String message, Object... args) {
        super(String.format(message, args));
    }
}
