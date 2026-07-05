package user.service.Mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class UserEmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public UserEmailService(JavaMailSender mailSender,
                            @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendWelcomeEmail(String to, String prenom, String loginUrl) {
        sendEmail(to, "Bienvenue !", loadWelcomeTemplate(prenom, loginUrl));
    }

    public void sendCessationEmail(String to, String prenom, String motif) {
        sendEmail(to, "Notification de désactivation", loadCessationTemplate(prenom, motif));
    }

    public void sendReactivationEmail(String to, String prenom) {
        sendEmail(to, "Réactivation de compte", loadReactivationTemplate(prenom));
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
            log.error("Erreur envoi email", e);
        }
    }

    private String loadWelcomeTemplate(String prenom, String loginUrl) {
        return loadTemplate("templates/welcome-email.html")
                .replace("{{prenom}}", prenom)
                .replace("{{loginUrl}}", loginUrl);
    }

    private String loadCessationTemplate(String prenom, String motif) {
        return loadTemplate("templates/cessation-email.html")
                .replace("{{prenom}}", prenom)
                .replace("{{motif}}", motif);
    }

    private String loadReactivationTemplate(String prenom) {
        return loadTemplate("templates/reactivation-email.html")
                .replace("{{prenom}}", prenom);
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
}