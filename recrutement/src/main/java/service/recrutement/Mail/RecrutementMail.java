package service.recrutement.Mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
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

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String frontendUrl;

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
        if (to == null || to.isBlank()) {
            log.warn("Email vide, non envoyé. Sujet={}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erreur envoi email à {}", to, e);
        }
    }

    // ============ HELPERS ============
    private String escape(String v) {
        return HtmlUtils.htmlEscape(v == null ? "" : v);
    }

    private String firstName(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) return "";
        return nomComplet.trim().split("\\s+")[0];
    }

    private String commentaireDisplay(String c) {
        return (c == null || c.isBlank()) ? "none" : "block";
    }

    private String lieuAffiche(Interview interview) {
        if (interview.getMode() == InterviewMode.PRESENTIEL) {
            return interview.getLieu() != null && !interview.getLieu().isBlank() ? interview.getLieu() : "Adresse communiquée séparément";
        }
        if (interview.getMode() == InterviewMode.MEET) {
            return interview.getLienVisio() != null && !interview.getLienVisio().isBlank() ? interview.getLienVisio() : "Lien communiqué séparément";
        }
        return "Vous serez contacté(e) par téléphone à l'heure indiquée";
    }

    private String libelleMode(InterviewMode mode) {
        if (mode == null) return "À préciser";
        return switch (mode) {
            case TELEPHONIQUE -> "Téléphonique";
            case MEET -> "Visioconférence Google Meet";
            case PRESENTIEL -> "Présentiel";
        };
    }

    private String libelleType(InterviewType type) {
        if (type == null) return "Entretien";
        return switch (type) {
            case RH_INITIAL -> "RH initial";
            case TECHNIQUE -> "technique";
            case RH_FINAL -> "RH final";
        };
    }

    // ============ NOUVEAU POSTE ============
    private String loadNewPosteRecrutementTemplate(String prenom, PosteRecrutement poste) {
        String consulterUrl = frontendUrl + "/postes/" + poste.getIdPosteRecrutement();
        return loadTemplate("templates/newPosteRecrutement.html")
                .replace("{{prenom}}", escape(prenom))
                .replace("{{titre}}", escape(poste.getTitre()))
                .replace("{{lieu}}", escape(poste.getLieu() == null ? "Non précisé" : poste.getLieu()))
                .replace("{{typeContrat}}", escape(poste.getTypeContrat() == null ? "Non précisé" : poste.getTypeContrat().toString()))
                .replace("{{description}}", escape(poste.getDescription()))
                .replace("{{consulterUrl}}", escape(consulterUrl));
    }

    public void sendNewPosteNotification(String toEmail, String prenom, PosteRecrutement poste) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Adresse email vide, notification ignorée pour le poste {}", poste.getIdPosteRecrutement());
            return;
        }
        String subject = "Nouveau poste disponible : " + poste.getTitre();
        sendEmail(toEmail, subject, loadNewPosteRecrutementTemplate(prenom, poste));
    }

    // ============ CANDIDATURE ============
    public void sendCandidatureConfirmation(PosteRecrutement poste, Application application) {
        if (application.getEmail() == null || application.getEmail().isBlank()) return;
        String html = loadTemplate("templates/NotifyUserthatHepostuledRecrutement.html")
                .replace("{{prenom}}", escape(firstName(application.getNomComplet())))
                .replace("{{titrePoste}}", escape(poste.getTitre()))
                .replace("{{consulterUrl}}", escape(frontendUrl + "/candidat/mes-candidatures"));
        sendEmail(application.getEmail(), "Candidature envoyée : " + poste.getTitre(), html);
    }

    public void sendNewApplicationNotificationToRH(String recruteurEmail, PosteRecrutement poste, Application application) {
        if (recruteurEmail == null || recruteurEmail.isBlank()) {
            log.info("Email RH indisponible pour poste {}", poste.getTitre());
            return;
        }
        String html = loadTemplate("templates/NotifyRHthatCandidatMAkeChangedInHisCandidat.html")
                .replace("{{titrePoste}}", escape(poste.getTitre()))
                .replace("{{nomCandidat}}", escape(application.getNomComplet()))
                .replace("{{emailCandidat}}", escape(application.getEmail()))
                .replace("{{consulterUrl}}", escape(frontendUrl + "/rh/candidatures/" + application.getIdApplication()));
        sendEmail(recruteurEmail, "Nouvelle candidature : " + poste.getTitre(), html);
    }

    // ============ ENTRETIEN ============
    public void sendEntretienConvocation(PosteRecrutement poste, Application application, Interview interview) {
        if (application.getEmail() == null || application.getEmail().isBlank()) return;
        String dateEntretien = interview.getDateEntretien() != null ? interview.getDateEntretien().format(DATE_FORMATTER) : "À confirmer prochainement";
        String html = loadTemplate("templates/NotifyUserThatHehasEntretien.html")
                .replace("{{prenom}}", escape(firstName(application.getNomComplet())))
                .replace("{{titrePoste}}", escape(poste.getTitre()))
                .replace("{{typeEntretien}}", escape(libelleType(interview.getType())))
                .replace("{{modeEntretien}}", escape(libelleMode(interview.getMode())))
                .replace("{{dateEntretien}}", escape(dateEntretien))
                .replace("{{lieuEntretien}}", escape(lieuAffiche(interview)))
                .replace("{{commentaireDisplay}}", "none")
                .replace("{{commentaireRH}}", "")
                .replace("{{consulterUrl}}", escape(frontendUrl + "/candidat/mes-candidatures"));
        sendEmail(application.getEmail(), "Convocation entretien " + libelleType(interview.getType()) + " : " + poste.getTitre(), html);
    }

    // ============ CHANGEMENT STATUT ============
    public void sendApplicationStatusChanged(PosteRecrutement poste, Application application) {
        if (application.getEmail() == null || application.getEmail().isBlank()) return;
        switch (application.getStatut()) {
            case REJETE -> sendRejete(poste, application);
            case EN_ENTRETIEN_TECHNIQUE -> sendEntretienRHReussi(poste, application);
            case EN_ENTRETIEN_FINAL -> sendEntretienTechniqueReussi(poste, application);
            case ACCEPTE -> sendCandidatAccepteFinal(poste, application);
            case SELECTIONNE, EN_ENTRETIEN_RH, RETIRE, EN_ATTENTE ->
                    log.debug("Aucun email automatique pour {}", application.getStatut());
        }
    }

    private void sendEntretienRHReussi(PosteRecrutement poste, Application application) {
        String c = application.getCommentaireRH();
        String html = loadTemplate("templates/NotifyUserThatCandidatAccepptedByRH.html")
                .replace("{{prenom}}", escape(firstName(application.getNomComplet())))
                .replace("{{titrePoste}}", escape(poste.getTitre()))
                .replace("{{commentaireDisplay}}", commentaireDisplay(c))
                .replace("{{commentaireRH}}", escape(c))
                .replace("{{consulterUrl}}", escape(frontendUrl + "/candidat/mes-candidatures"));
        sendEmail(application.getEmail(), "Entretien RH réussi : " + poste.getTitre(), html);
    }

    private void sendEntretienTechniqueReussi(PosteRecrutement poste, Application application) {
        String c = application.getCommentaireRH();
        String html = loadTemplate("templates/NotifyUserThatCandidatAccepptedByEmplyee.html")
                .replace("{{prenom}}", escape(firstName(application.getNomComplet())))
                .replace("{{titrePoste}}", escape(poste.getTitre()))
                .replace("{{commentaireDisplay}}", commentaireDisplay(c))
                .replace("{{commentaireRH}}", escape(c))
                .replace("{{consulterUrl}}", escape(frontendUrl + "/candidat/mes-candidatures"));
        sendEmail(application.getEmail(), "Entretien technique réussi : " + poste.getTitre(), html);
    }

    private void sendCandidatAccepteFinal(PosteRecrutement poste, Application application) {
        String c = application.getCommentaireRH();
        String html = loadTemplate("templates/NotifyUserThatCandidatAcceppted.html")
                .replace("{{prenom}}", escape(firstName(application.getNomComplet())))
                .replace("{{titrePoste}}", escape(poste.getTitre()))
                .replace("{{commentaireDisplay}}", commentaireDisplay(c))
                .replace("{{commentaireRH}}", escape(c))
                .replace("{{consulterUrl}}", escape(frontendUrl + "/candidat/mes-candidatures"));
        sendEmail(application.getEmail(), "Félicitations, vous êtes recruté(e) : " + poste.getTitre(), html);
    }

    private void sendRejete(PosteRecrutement poste, Application application) {
        String c = application.getCommentaireRH();
        String html = loadTemplate("templates/NotifyUserThatCandidatRejected.html")
                .replace("{{prenom}}", escape(firstName(application.getNomComplet())))
                .replace("{{titrePoste}}", escape(poste.getTitre()))
                .replace("{{commentaireDisplay}}", commentaireDisplay(c))
                .replace("{{commentaireRH}}", escape(c))
                .replace("{{consulterUrl}}", escape(frontendUrl + "/candidat/ListePosteRecrutement"));
        sendEmail(application.getEmail(), "Mise à jour de votre candidature : " + poste.getTitre(), html);
    }
}