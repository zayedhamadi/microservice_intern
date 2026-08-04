package service.recrutement.Mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import service.recrutement.Entity.PosteRecrutement;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class RecrutementMail {
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
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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
        String html = loadNewPosteRecrutementTemplate(prenom, poste);
        sendEmail(toEmail, subject, html);
    }
}