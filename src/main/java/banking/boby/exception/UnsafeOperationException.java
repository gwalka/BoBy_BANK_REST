package banking.boby.exception;

public class UnsafeOperationException extends RuntimeException {

    public UnsafeOperationException(String message, Object... args) {
        super(String.format(message, args));
    }
}