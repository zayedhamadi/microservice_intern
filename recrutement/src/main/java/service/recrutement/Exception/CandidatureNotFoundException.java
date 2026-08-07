package service.recrutement.Exception;

public class CandidatureNotFoundException extends RuntimeException {
    public CandidatureNotFoundException(String id) {
        super("Candidature introuvable : " + id);
    }
}