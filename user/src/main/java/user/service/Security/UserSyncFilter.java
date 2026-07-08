package user.service.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import user.service.Entity.Enum.Compte;
import user.service.Entity.User;
import user.service.Repository.UserRepository;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserSyncFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

        private final Set<String> knownSyncedIds = ConcurrentHashMap.newKeySet();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String keycloakId = jwt.getSubject();

            if (keycloakId != null && !knownSyncedIds.contains(keycloakId)) {
                if (userRepository.existsByKeycloakId(keycloakId)) {
                    knownSyncedIds.add(keycloakId);
                } else {
                    syncUserFromJwt(jwt);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void syncUserFromJwt(Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");

            if (email == null || email.isBlank()) {
                log.warn("Auto-sync impossible : JWT sans email pour keycloakId={}", keycloakId);
                return;
            }

            if (userRepository.existsByEmail(email)) {
                // Un autre compte utilise déjà cet email avec un keycloakId différent :
                // situation anormale à investiguer manuellement, on n'écrase rien.
                log.warn("Auto-sync ignoré : email {} déjà utilisé par un autre compte", email);
                return;
            }

            User user = User.builder()
                    .keycloakId(keycloakId)
                    .email(email)
                    .nom(orEmpty(jwt.getClaimAsString("family_name")))
                    .prenom(orEmpty(jwt.getClaimAsString("given_name")))
                    .etatCompte(Compte.ACTIF)
                    .build();

            userRepository.save(user);
            knownSyncedIds.add(keycloakId);
            log.info("Auto-sync : utilisateur créé en DB depuis JWT (keycloakId={}, email={})", keycloakId, email);

        } catch (Exception e) {
            log.error("Erreur auto-sync utilisateur depuis JWT : {}", e.getMessage(), e);
        }
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }
}