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