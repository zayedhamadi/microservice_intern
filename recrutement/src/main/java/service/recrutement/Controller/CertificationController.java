package service.recrutement.Controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Client.UserServiceClient;
import service.recrutement.Entity.dto.CertificationDto;
import service.recrutement.Entity.dto.PageResponseDto;
import service.recrutement.Service.CertificationService;

import java.util.List;
@PreAuthorize("hasAnyRole('EMPLOYEE','CANDIDAT','RH')")
@RestController
@RequestMapping("/certifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificationController {
    UserServiceClient userServiceClient;
    CertificationService certificationService;

    // BUG FIX : le path variable était "userId" alors que le path déclare "{keycloakId}"
    // -> Spring ne matchait jamais. On garde la route par userId numérique (legacy)
    // mais on résout d'abord le keycloakId.
    @GetMapping("/admin/user-by-id/{userId}/certifications")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<List<CertificationDto>> getCertificationsByUserId(@PathVariable Long userId) {
        String keycloakId = userServiceClient.getKeycloakIdByUserId(userId);
        return ResponseEntity.ok(certificationService.getByUserId(keycloakId));
    }

    // Nouveau : version paginée + filtrable, directement par keycloakId (celle à utiliser côté RH)
    @GetMapping("/admin/user/{keycloakId}/certifications/page")
    @PreAuthorize("hasAnyRole('RH','EMPLOYEE')")
    public ResponseEntity<PageResponseDto<CertificationDto>> getCertificationsByUserIdPaged(
            @PathVariable String keycloakId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "dateCertif") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String titre) {

        return ResponseEntity.ok(
                certificationService.getByUserIdPaged(keycloakId, page, size, sortBy, sortDir, titre)
        );
    }

    @GetMapping("/getMine")
    public ResponseEntity<List<CertificationDto>> getMyCertifications(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(certificationService.getMyCertifications(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificationDto> getById(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable String id) {
        return ResponseEntity.ok(certificationService.getById(id, jwt.getSubject()));
    }

    @PostMapping("/add")
    public ResponseEntity<CertificationDto> add(@AuthenticationPrincipal Jwt jwt,
                                                @RequestBody CertificationDto dto) {
        return ResponseEntity.ok(certificationService.add(jwt.getSubject(), dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CertificationDto> update(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable String id,
                                                   @RequestBody CertificationDto dto) {
        return ResponseEntity.ok(certificationService.update(id, jwt.getSubject(), dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        certificationService.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}