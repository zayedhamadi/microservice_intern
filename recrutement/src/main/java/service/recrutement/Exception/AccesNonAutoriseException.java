package service.recrutement.Exception;

public class AccesNonAutoriseException extends RuntimeException {
    public AccesNonAutoriseException(String message) {
        super(message);
    }
}