package user.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.AuthResponse;
import user.service.Dto.LoginRequest;
import user.service.Dto.RegisterRequest;
import user.service.Dto.UpdateUSerAfterConnect;
import user.service.Entity.User;
import user.service.Serivce.UserService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Compte créé avec succès",
                    "id", user.getId(),
                    "email", user.getEmail()
            ));
        } catch (RuntimeException e) {
            log.error("Erreur register : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Erreur login : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /** Appelé par Angular juste après le retour de Keycloak (token Google échangé). */
    @PostMapping("/google/sync")
    public ResponseEntity<?> syncGoogleUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String prenom = jwt.getClaimAsString("given_name");
            String nom = jwt.getClaimAsString("family_name");

            log.info("Google sync pour : {}", email);
            User user = userService.syncGoogleUser(keycloakId, email, nom, prenom);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("nom", user.getNom());
            response.put("prenom", user.getPrenom());
            response.put("role", user.getRole() != null ? user.getRole().name() : null);
            response.put("keycloakId", user.getKeycloakId());
            response.put("profileComplete", user.getRole() != null);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Erreur Google sync : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/google/url")
    public ResponseEntity<?> getGoogleLoginUrl() {
        String url = keycloakUrl + "/realms/" + realm
                + "/protocol/openid-connect/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=http://localhost:4200/callback"
                + "&response_type=code"
                + "&scope=openid profile email"
                + "&kc_idp_hint=google";

        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Complète le profil après signup (Google ou classique). */
    @PutMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody UpdateUSerAfterConnect dto) {
        try {
            String keycloakId = jwt.getSubject();
            User user = userService.completeProfile(keycloakId, dto);
            return ResponseEntity.ok(Map.of(
                    "message", "Profil complété",
                    "role", user.getRole().name()
            ));
        } catch (RuntimeException e) {
            log.error("Erreur complete-profile : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}