package banking.boby.exception;

public class WrongCardOperationException extends RuntimeException {

    public WrongCardOperationException(String message, Object... args) {
        super(String.format(message, args));
    }
}
