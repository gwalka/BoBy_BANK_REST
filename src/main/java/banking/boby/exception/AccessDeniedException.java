package banking.boby.exception;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message, Object... args) {
        super(String.format(message, args));
    }
}
