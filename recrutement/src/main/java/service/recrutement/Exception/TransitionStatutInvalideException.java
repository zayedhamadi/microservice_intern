package service.recrutement.Exception;

public class TransitionStatutInvalideException extends RuntimeException {
    public TransitionStatutInvalideException(String message) {
        super(message);
    }
}