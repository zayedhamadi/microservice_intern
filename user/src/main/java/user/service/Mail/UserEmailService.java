package user.service.Mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    private String loadReactivationTemplate(String prenom) {
        return loadTemplate("templates/reactivation-email.html")
                .replace("{{prenom}}", prenom);
    }

    private String loadCessationTemplate(String prenom, String motif) {
        return loadTemplate("templates/cessation-email.html")
                .replace("{{prenom}}", prenom)
                .replace("{{motif}}", motif);
    }

    public void sendCessationEmail(String to, String prenom, String motif) {
        try {
            String html = loadCessationTemplate(prenom, motif);

            sendEmail(to, "Votre compte a été désactivé ⚠️", html);

            log.info(" Email cessation envoyé à {}", to);

        } catch (Exception e) {
            log.error(" Erreur email cessation", e);
        }
    }

    public void sendReactivationEmail(String to, String prenom) {
        try {
            String html = loadReactivationTemplate(prenom);

            sendEmail(to, "Votre compte est réactivé ", html);

            log.info("Email réactivation envoyé à {}", to);

        } catch (Exception e) {
            log.error(" Erreur email réactivation", e);
        }
    }

    public void sendWelcomeEmail(String to, String prenom, String loginUrl) {
        sendEmail(to, "Bienvenue !", loadTemplate("templates/welcome-email.html")
                .replace("{{prenom}}", prenom)
                .replace("{{loginUrl}}", loginUrl));
    }

    public void sendResetPasswordEmail(String to, String prenom, String resetLink) {
        sendEmail(to, "Réinitialisation de mot de passe", loadTemplate("templates/reset-password-email.html")
                .replace("{{prenom}}", prenom)
                .replace("{{resetLink}}", resetLink));
    }

    public void sendPasswordChangedEmail(String to, String prenom) {
        sendEmail(to, "Mot de passe modifié", loadTemplate("templates/password-changed-email.html")
                .replace("{{prenom}}", prenom));
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