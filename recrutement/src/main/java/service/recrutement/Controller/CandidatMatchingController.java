package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.dto.PosteClasseDto;
import service.recrutement.Security.JwtExtractService;
import service.recrutement.Service.MatchingService;

import java.util.List;

@RestController
@RequestMapping("/candidat/postes")
@RequiredArgsConstructor
public class CandidatMatchingController {

    private final MatchingService matchingService;
    private final JwtExtractService jwtExtractService;

    @GetMapping("/recommandes")
    @PreAuthorize("hasRole('CANDIDAT')")
    public ResponseEntity<List<PosteClasseDto>> getPostesRecommandes(Authentication authentication) {
        String candidatKeycloakId = jwtExtractService.extractKeycloakId(authentication);
        return ResponseEntity.ok(matchingService.getPostesClassesPourCandidat(candidatKeycloakId));
    }
}