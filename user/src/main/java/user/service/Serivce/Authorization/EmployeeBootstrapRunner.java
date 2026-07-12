package user.service.Serivce.Authorization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import user.service.Entity.Enum.Role;
import user.service.Repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.employee.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.employee.email}")
    private String email;

    @Value("${app.bootstrap.employee.password}")
    private String password;

    @Value("${app.bootstrap.employee.nom}")
    private String nom;

    @Value("${app.bootstrap.employee.prenom}")
    private String prenom;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Bootstrap EMPLOYEE désactivé (app.bootstrap.employee.enabled=false)");
            return;
        }

        boolean employeeExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == Role.EMPLOYEE);

        if (employeeExists) {
            log.info("Un compte EMPLOYEE existe déjà, bootstrap ignoré.");
            return;
        }

        try {
            userService.createBootstrapEmployee(email, password, nom, prenom);
            log.info("Compte EMPLOYEE bootstrap créé avec succès : {}", email);
        } catch (Exception e) {
            log.error("Échec création du compte EMPLOYEE bootstrap : {}", e.getMessage(), e);
        }
    }
}