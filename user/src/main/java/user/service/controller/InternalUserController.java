package user.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import user.service.Entity.User;
import user.service.Repository.UserRepository;
import user.service.Serivce.UserCommun.UserFinderService;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserFinderService userFinderService;
    private final UserRepository userRepository;

    @GetMapping("/{userId}/keycloak-id")
    public ResponseEntity<String> getKeycloakIdByUserId(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user.getKeycloakId());
    }

    @GetMapping("/{keycloakId}/candidat-info")
    public ResponseEntity<?> getCandidatInfo(@PathVariable String keycloakId) {
        User user = userFinderService.getUserByKeycloakId(keycloakId);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("keycloakId", user.getKeycloakId());
        response.put("nom", user.getNom());
        response.put("prenom", user.getPrenom());
        response.put("email", user.getEmail());
        return ResponseEntity.ok(response);
    }
}