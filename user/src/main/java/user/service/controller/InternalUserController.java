package user.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import user.service.Entity.User;
import user.service.Serivce.UserCommun.UserFinderService;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserFinderService userFinderService;

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