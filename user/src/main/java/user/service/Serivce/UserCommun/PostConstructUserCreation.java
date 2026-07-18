package user.service.Serivce.UserCommun;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import user.service.Serivce.Authorization.UserService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostConstructUserCreation {

    private final UserService userService;
    private final UserFinderService userFinderService;

    @Value("${app.bootstrap.employee.enabled}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.employee.email}")
    private String bootstrapEmail;

    @Value("${app.bootstrap.employee.password}")
    private String bootstrapPassword;

    @Value("${app.bootstrap.employee.nom}")
    private String bootstrapNom;

    @Value("${app.bootstrap.employee.prenom}")
    private String bootstrapPrenom;

    @PostConstruct
    public void saveAdmin() {
        if (!bootstrapEnabled) {
            log.info("Bootstrap admin désactivé (app.bootstrap.employee.enabled=false)");
            return;
        }

        if (userFinderService.existsByEmail(bootstrapEmail)) {
            log.info("Compte bootstrap déjà existant : {}", bootstrapEmail);
            return;
        }

        try {
            userService.createBootstrapEmployee(
                    bootstrapEmail,
                    bootstrapPassword,
                    bootstrapNom,
                    bootstrapPrenom
            );
            log.info("Compte admin bootstrap créé avec succès : {}", bootstrapEmail);
        } catch (Exception e) {
            log.error("Échec de la création du compte admin bootstrap : {}", e.getMessage());
        }
    }
}