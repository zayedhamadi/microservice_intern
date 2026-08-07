package service.recrutement.Exception;

public class CandidatureExistanteException extends RuntimeException {
    public CandidatureExistanteException() {
        super("Vous avez déjà postulé à ce poste");
    }
}