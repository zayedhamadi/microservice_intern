package service.recrutement.Exception;

public class DemandeReportNotFoundException extends RuntimeException {
    public DemandeReportNotFoundException(String id) {
        super("Demande de report introuvable : " + id);
    }
}