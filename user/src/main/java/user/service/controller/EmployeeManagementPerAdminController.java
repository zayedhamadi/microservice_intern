package user.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.service.Dto.createUserPerAdminDto;
import user.service.Entity.User;
import user.service.Serivce.Admin.EmployeeManagement;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/EmployeeManagementController")
@RequiredArgsConstructor
public class EmployeeManagementPerAdminController {

    private final EmployeeManagement employeeManagement;

    @PostMapping("/admin/register")
    public ResponseEntity<?> register(@Valid @RequestBody createUserPerAdminDto request) {
        try {
            // employeeManagement.register() gère déjà la création Keycloak + DB
            // + l'envoi de l'email avec identifiants (password + matricule).
            // Pas besoin d'un 2e email de bienvenue ici.
            User user = this.employeeManagement.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Compte créé avec succès",
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "matricule", user.getMatricule()
            ));
        } catch (RuntimeException e) {
            log.error("Erreur register (admin) : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}