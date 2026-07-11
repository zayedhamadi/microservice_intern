package user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.CertificationDTO;
import user.service.Entity.User;
import user.service.Repository.UserRepository;
import user.service.Serivce.CertificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/certifications")
@PreAuthorize("hasAnyRole('CANDIDAT', 'RH', 'EMPLOYEE')")
public class CertificationController {

    private final CertificationService certificationService;
    private final UserRepository userRepository;

    // ❌ retiré @GetMapping ici : ce n'est pas un endpoint, juste un helper interne.
    // Le laisser mappé créait un conflit de route avec les autres GET de ce contrôleur.
    private Long getConnectedUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));
        return user.getId();
    }

    @PostMapping("/add")
    public ResponseEntity<CertificationDTO> add(
            Authentication authentication,
            @RequestBody CertificationDTO dto) {
        Long userId = getConnectedUserId(authentication);
        return ResponseEntity.ok(certificationService.addCertification(userId, dto));
    }

    @GetMapping("/getMine")
    public ResponseEntity<List<CertificationDTO>> getMine(Authentication authentication) {
        Long userId = getConnectedUserId(authentication);
        return ResponseEntity.ok(certificationService.getMyCertifications(userId));
    }

    // ✅ corrigé : était "/certifications/{id}" → doublait le préfixe de classe
    @GetMapping("/{id}")
    public ResponseEntity<CertificationDTO> getOne(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getConnectedUserId(authentication);
        return ResponseEntity.ok(certificationService.getCertificationById(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CertificationDTO> update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody CertificationDTO dto) {
        Long userId = getConnectedUserId(authentication);
        return ResponseEntity.ok(certificationService.updateCertification(id, userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getConnectedUserId(authentication);
        certificationService.deleteCertification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/certifications")
    public ResponseEntity<List<CertificationDTO>> getAll() {
        return ResponseEntity.ok(certificationService.getAllCertifications());
    }

    @GetMapping("/admin/certifications/user/{userId}")
    public ResponseEntity<List<CertificationDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(certificationService.getCertificationsByUserId(userId));
    }
}