package service.recrutement.Controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.dto.CertificationDto;
import service.recrutement.Service.CertificationService;

import java.util.List;

@RestController
@RequestMapping("/certifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificationController {

    CertificationService certificationService;

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