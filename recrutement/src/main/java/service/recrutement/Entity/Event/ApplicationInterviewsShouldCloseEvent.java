package service.recrutement.Entity.Event;

import lombok.Getter;

/**
 * Publié par ApplyService quand une candidature quitte son cycle actuel
 * (retrait par le candidat, ou redépôt après retrait) et que tout entretien
 * encore actif (PLANIFIE/REPORTE) rattaché à cette candidature doit être
 * clôturé automatiquement.
 *
 * BUG FIX : avant ce correctif, rien ne fermait les entretiens actifs lors
 * d'un retrait de candidature ni lors d'un redépôt (qui réutilise le même
 * idApplication) — ce qui pouvait laisser un entretien PLANIFIE fantôme
 * bloquant définitivement la création d'un nouvel entretien du même type
 * (via l'index unique activeSlotKey) et mélanger l'historique de deux
 * cycles de candidature.
 *
 * Utiliser un événement plutôt qu'un appel direct ApplyService -> InterviewService
 * évite une dépendance circulaire (InterviewService dépend déjà d'ApplyService).
 */
@Getter
public class ApplicationInterviewsShouldCloseEvent {

    private final String applicationId;
    private final String motif;

    public ApplicationInterviewsShouldCloseEvent(String applicationId, String motif) {
        this.applicationId = applicationId;
        this.motif = motif;
    }
}