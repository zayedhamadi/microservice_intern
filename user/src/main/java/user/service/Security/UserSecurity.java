package user.service.Security;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import user.service.Entity.User;
import user.service.Repository.UserRepository;
import user.service.Serivce.UserFinderService;


import java.util.Optional;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    public boolean isOwner(Authentication authentication, Long userId) {
        if (authentication == null) return false;

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        return userRepository.findById(userId)
                .map(u -> u.getKeycloakId().equals(keycloakId))
                .orElse(false);
    }
}