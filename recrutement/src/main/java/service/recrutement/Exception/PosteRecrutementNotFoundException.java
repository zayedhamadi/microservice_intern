package service.recrutement.Exception;

public class PosteRecrutementNotFoundException extends RuntimeException {
    public PosteRecrutementNotFoundException(String posteId) {
        super("Poste de recrutement introuvable : " + posteId);
    }
}