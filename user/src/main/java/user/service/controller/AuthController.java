package user.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.*;
import user.service.Entity.PasswordResetToken;
import user.service.Entity.User;
import user.service.Mail.UserEmailService;
import user.service.Repository.PasswordResetTokenRepository;
import user.service.Repository.UserRepository;
import user.service.Serivce.Authorization.KeycloakService;
import user.service.Serivce.Authorization.TokenBlacklistService;
import user.service.Serivce.Authorization.UserService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final KeycloakService keycloakService;
    private final JwtDecoder jwtDecoder;
    private final UserEmailService userEmailService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Jwt jwt) {
        try {
            String jti = jwt.getId();
            Instant expiresAt = jwt.getExpiresAt();

            if (jti != null && expiresAt != null) {
                tokenBlacklistService.blacklist(jti, expiresAt);
            }

            return ResponseEntity.ok(Map.of("message", "Déconnexion effectuée, token révoqué."));
        } catch (Exception e) {
            log.error("Erreur logout : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur lors de la déconnexion."));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgetPwRequest request) {
        log.info("Forgot password appelé pour {}", request.getEmail());

        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                tokenRepository.deleteByEmail(request.getEmail());

                String token = java.util.UUID.randomUUID().toString();
                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .token(token)
                        .keycloakId(user.getKeycloakId())
                        .email(request.getEmail())
                        .expiresAt(java.time.LocalDateTime.now().plusMinutes(30))
                        .used(false)
                        .build();

                tokenRepository.save(resetToken);

                String resetLink = "http://localhost:4200/reset-password?token=" + token;
                userEmailService.sendResetPasswordEmail(user.getEmail(), user.getPrenom(), resetLink);
                log.info("Email reset envoyé à {}", request.getEmail());
            }

            return ResponseEntity.ok(Map.of("message", "Si cet email existe, vous recevrez un lien."));
        } catch (Exception e) {
            log.error("Erreur forgot-password : {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("message", "Si cet email existe, vous recevrez un lien."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPwRequest request) {
        try {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mots de passe différents"));
            }

            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(request.getToken());
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lien invalide"));
            }

            PasswordResetToken resetToken = tokenOpt.get();

            if (resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                tokenRepository.delete(resetToken);
                return ResponseEntity.badRequest().body(Map.of("error", "Lien expiré. Faites une nouvelle demande."));
            }
            if (resetToken.isUsed()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ce lien a déjà été utilisé."));
            }

            keycloakService.changePassword(resetToken.getKeycloakId(), request.getPassword());
            tokenRepository.delete(resetToken);

            userRepository.findByEmail(resetToken.getEmail())
                    .ifPresent(user -> userEmailService.sendPasswordChangedEmail(user.getEmail(), user.getPrenom()));

            log.info("Password reset réussi pour {}", resetToken.getEmail());
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès !"));
        } catch (Exception e) {
            log.error("Erreur reset-password : {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur lors du reset : " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request); // lève déjà l'exception si EMPLOYEE
            userEmailService.sendWelcomeEmail(user.getEmail(), user.getPrenom(), "http://localhost:4200/login");
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

    @PostMapping("/google/callback")
    public ResponseEntity<?> googleCallback(@RequestParam String code) {
        try {
            Map<String, Object> tokenData = keycloakService.exchangeAuthorizationCode(
                    code, "http://localhost:4200/callback"
            );

            String accessToken = (String) tokenData.get("access_token");
            Jwt jwt = jwtDecoder.decode(accessToken);

            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String prenom = jwt.getClaimAsString("given_name");
            String nom = jwt.getClaimAsString("family_name");

            User user = userService.syncGoogleUser(keycloakId, email, nom, prenom);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", tokenData.get("refresh_token"));
            response.put("tokenType", "Bearer");
            response.put("expiresIn", tokenData.get("expires_in"));
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("nom", user.getNom());
            response.put("prenom", user.getPrenom());
            response.put("role", user.getRole() != null ? user.getRole().name() : null);
            response.put("keycloakId", user.getKeycloakId());
            response.put("profileComplete", user.getRole() != null);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Erreur Google callback : {}", e.getMessage());
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
            response.put("nom", user.getNom() != null ? user.getNom() : "");
            response.put("prenom", user.getPrenom() != null ? user.getPrenom() : "");
            response.put("genre", user.getGenre() != null ? user.getGenre().name() : null);
            response.put("num_Tel", user.getNum_Tel());
            response.put("role", user.getRole() != null ? user.getRole().name() : null);
            response.put("etatCompte", user.getEtatCompte().name());
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


}