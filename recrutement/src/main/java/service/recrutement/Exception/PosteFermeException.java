package service.recrutement.Exception;

public class PosteFermeException extends RuntimeException {
    public PosteFermeException(String titre) {
        super("Ce poste n'accepte plus de candidatures : " + titre);
    }
}