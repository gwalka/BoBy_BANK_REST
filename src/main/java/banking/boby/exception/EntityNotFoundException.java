package banking.boby.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message, Object... args) {
        super(String.format(message, args));
    }
}
