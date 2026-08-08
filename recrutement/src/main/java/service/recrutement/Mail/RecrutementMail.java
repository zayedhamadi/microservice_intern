package service.recrutement.Mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.InterviewMode;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.PosteRecrutement;

import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class RecrutementMail {
    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    public RecrutementMail(JavaMailSender mailSender,
                           @Value("${spring.mail.username}") String fromEmail,
                           @Value("${app.frontend.url:http://localhost:4200}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.frontendUrl = frontendUrl;
    }

    private String loadTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return content.replace("{{anneeCourante}}", String.valueOf(Year.now().getValue()));
        } catch (Exception e) {
            log.error("Erreur lecture template : {}", path, e);
            return "<h1>Erreur template email</h1>";
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erreur envoi email à {}", to, e);
        }
    }

    private String firstName(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) return "";
        return nomComplet.split(" ")[0];
    }

    private String loadNewPosteRecrutementTemplate(String prenom, PosteRecrutement poste) {
        String consulterUrl = frontendUrl + "/postes/" + poste.getIdPosteRecrutement();

        return loadTemplate("templates/newPosteRecrutement.html")
                .replace("{{prenom}}", prenom == null ? "" : prenom)
                .replace("{{titre}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{lieu}}", poste.getLieu() == null ? "Non précisé" : poste.getLieu())
                .replace("{{typeContrat}}", poste.getTypeContrat() == null ? "Non précisé" : poste.getTypeContrat().toString())
                .replace("{{description}}", poste.getDescription() == null ? "" : poste.getDescription())
                .replace("{{consulterUrl}}", consulterUrl);
    }

    public void sendNewPosteNotification(String toEmail, String prenom, PosteRecrutement poste) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Adresse email vide, notification ignorée pour le poste {}", poste.getIdPosteRecrutement());
            return;
        }
        String subject = "Nouveau poste disponible : " + poste.getTitre();
        sendEmail(toEmail, subject, loadNewPosteRecrutementTemplate(prenom, poste));
    }

    public void sendCandidatureConfirmation(PosteRecrutement poste, Application application) {
        if (application.getEmail() == null || application.getEmail().isBlank()) {
            log.warn("Email candidat vide, confirmation de candidature ignorée pour {}", application.getIdApplication());
            return;
        }

        String consulterUrl = frontendUrl + "/candidat/mes-candidatures";
        String html = loadTemplate("templates/NotifyUserthatHepostuledRecrutement.html")
                .replace("{{prenom}}", firstName(application.getNomComplet()))
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(application.getEmail(), "Candidature envoyée : " + poste.getTitre(), html);
    }

    public void sendNewApplicationNotificationToRH(String recruteurEmail, PosteRecrutement poste, Application application) {
        if (recruteurEmail == null || recruteurEmail.isBlank()) {
            log.info("Email recruteur indisponible — notification RH loguée uniquement. Poste='{}', candidat={}, email candidat={}",
                    poste.getTitre(), application.getCandidatKeycloakId(), application.getEmail());
            return;
        }

        String consulterUrl = frontendUrl + "/rh/candidatures/" + application.getIdApplication();
        String html = loadTemplate("templates/NotifyRHthatCandidatMAkeChangedInHisCandidat.html")
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{nomCandidat}}", application.getNomComplet() == null ? "Candidat" : application.getNomComplet())
                .replace("{{emailCandidat}}", application.getEmail() == null ? "" : application.getEmail())
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(recruteurEmail, "Nouvelle candidature : " + poste.getTitre(), html);
    }

    /** Convocation à un entretien — le contenu dépend du mode (téléphone / Meet / présentiel). */
    public void sendEntretienConvocation(PosteRecrutement poste, Application application, Interview interview) {
        if (application.getEmail() == null || application.getEmail().isBlank()) {
            log.warn("Email candidat vide, convocation ignorée pour {}", application.getIdApplication());
            return;
        }

        String consulterUrl = frontendUrl + "/candidat/mes-candidatures";

        String dateEntretien = interview.getDateEntretien() != null
                ? interview.getDateEntretien().format(DATE_FORMATTER)
                : "À confirmer prochainement";

        String typeEntretien = libelleType(interview.getType());
        String modeEntretien = libelleMode(interview.getMode());
        String lieuEntretien = lieuAffiche(interview);

        String html = loadTemplate("templates/NotifyUserThatHehasEntretien.html")
                .replace("{{prenom}}", firstName(application.getNomComplet()))
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{typeEntretien}}", typeEntretien)
                .replace("{{modeEntretien}}", modeEntretien)
                .replace("{{dateEntretien}}", dateEntretien)
                .replace("{{lieuEntretien}}", lieuEntretien)
                .replace("{{commentaireDisplay}}", "none")
                .replace("{{commentaireRH}}", "")
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(application.getEmail(), "Entretien " + typeEntretien + " planifié : " + poste.getTitre(), html);
    }

    private String lieuAffiche(Interview interview) {
        if (interview.getMode() == InterviewMode.PRESENTIEL) {
            return interview.getLieu() != null && !interview.getLieu().isBlank()
                    ? interview.getLieu() : "Adresse communiquée séparément";
        }
        if (interview.getMode() == InterviewMode.MEET) {
            return interview.getLienVisio() != null && !interview.getLienVisio().isBlank()
                    ? interview.getLienVisio() : "Lien communiqué séparément";
        }
        return "Vous serez contacté(e) par téléphone à l'heure indiquée";
    }

    private String libelleMode(InterviewMode mode) {
        if (mode == null) return "À préciser";
        return switch (mode) {
            case TELEPHONIQUE -> "Téléphonique";
            case MEET -> "Visioconférence (Google Meet)";
            case PRESENTIEL -> "Présentiel";
        };
    }

    public void sendApplicationStatusChanged(PosteRecrutement poste, Application application) {
        if (application.getEmail() == null || application.getEmail().isBlank()) {
            log.warn("Email candidat vide, notification de statut ignorée pour {}", application.getIdApplication());
            return;
        }

        switch (application.getStatut()) {
            case SELECTIONNE -> sendSelectionne(poste, application);
            case EN_ENTRETIEN_TECHNIQUE -> sendEntretienRHReussi(poste, application);
            case EN_ENTRETIEN_FINAL -> sendEntretienTechniqueReussi(poste, application);
            case ACCEPTE -> sendCandidatAccepteFinal(poste, application);
            case REJETE -> sendRejete(poste, application);
            case EN_ENTRETIEN_RH, RETIRE, EN_ATTENTE ->
                    log.debug("Aucun email de statut envoyé pour {}", application.getStatut());
        }
    }

    private String libelleType(InterviewType type) {
        return switch (type) {
            case RH_INITIAL -> "RH";
            case TECHNIQUE -> "technique";
            case RH_FINAL -> "RH final";
        };
    }

    private String commentaireDisplay(String commentaire) {
        return (commentaire == null || commentaire.isBlank()) ? "none" : "block";
    }

    private void sendSelectionne(PosteRecrutement poste, Application application) {
        String consulterUrl = frontendUrl + "/candidat/mes-candidatures";
        String commentaire = application.getCommentaireRH();

        String html = loadTemplate("templates/notifyUserThatStatusposteChanged.html")
                .replace("{{prenom}}", firstName(application.getNomComplet()))
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{commentaireDisplay}}", commentaireDisplay(commentaire))
                .replace("{{commentaireRH}}", commentaire == null ? "" : commentaire)
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(application.getEmail(), "Votre profil a été sélectionné", html);
    }

    private void sendEntretienRHReussi(PosteRecrutement poste, Application application) {
        String consulterUrl = frontendUrl + "/candidat/mes-candidatures";
        String commentaire = application.getCommentaireRH();

        String html = loadTemplate("templates/NotifyUserThatCandidatAccepptedByRH.html")
                .replace("{{prenom}}", firstName(application.getNomComplet()))
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{commentaireDisplay}}", commentaireDisplay(commentaire))
                .replace("{{commentaireRH}}", commentaire == null ? "" : commentaire)
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(application.getEmail(), "Entretien RH réussi : " + poste.getTitre(), html);
    }

    private void sendEntretienTechniqueReussi(PosteRecrutement poste, Application application) {
        String consulterUrl = frontendUrl + "/candidat/mes-candidatures";
        String commentaire = application.getCommentaireRH();

        String html = loadTemplate("templates/NotifyUserThatCandidatAccepptedByEmplyee.html")
                .replace("{{prenom}}", firstName(application.getNomComplet()))
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{commentaireDisplay}}", commentaireDisplay(commentaire))
                .replace("{{commentaireRH}}", commentaire == null ? "" : commentaire)
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(application.getEmail(), "Entretien technique réussi : " + poste.getTitre(), html);
    }

    private void sendCandidatAccepteFinal(PosteRecrutement poste, Application application) {
        String consulterUrl = frontendUrl + "/candidat/mes-candidatures";
        String commentaire = application.getCommentaireRH();

        String html = loadTemplate("templates/NotifyUserThatCandidatAcceppted.html")
                .replace("{{prenom}}", firstName(application.getNomComplet()))
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{commentaireDisplay}}", commentaireDisplay(commentaire))
                .replace("{{commentaireRH}}", commentaire == null ? "" : commentaire)
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(application.getEmail(), "Félicitations, vous êtes recruté(e) : " + poste.getTitre(), html);
    }

    private void sendRejete(PosteRecrutement poste, Application application) {
        String consulterUrl = frontendUrl + "/candidat/ListePosteRecrutement";
        String commentaire = application.getCommentaireRH();

        String html = loadTemplate("templates/NotifyUserThatCandidatRejected.html")
                .replace("{{prenom}}", firstName(application.getNomComplet()))
                .replace("{{titrePoste}}", poste.getTitre() == null ? "" : poste.getTitre())
                .replace("{{commentaireDisplay}}", commentaireDisplay(commentaire))
                .replace("{{commentaireRH}}", commentaire == null ? "" : commentaire)
                .replace("{{consulterUrl}}", consulterUrl);

        sendEmail(application.getEmail(), "Mise à jour de votre candidature : " + poste.getTitre(), html);
    }
}