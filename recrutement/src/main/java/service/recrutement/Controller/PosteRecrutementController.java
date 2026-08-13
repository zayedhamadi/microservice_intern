package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.PosteAvecCandidaturesDto;
import service.recrutement.Entity.dto.PosteRecrutementDto;
import service.recrutement.Service.PosteRecrutementService;
import service.recrutement.Service.PosteRecrutementServiceExtrait;

import java.util.List;

@RestController
@RequestMapping("/rh/api/postesRecrutement")
@RequiredArgsConstructor
public class PosteRecrutementController {

    private final PosteRecrutementService posteRecrutementService;
    private final PosteRecrutementServiceExtrait posteRecrutementServiceExtrait;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','RH','CANDIDAT')")
    public ResponseEntity<List<PosteRecrutement>> getAllPostes() {
        return ResponseEntity.ok(posteRecrutementService.getAllPostes());
    }

    @GetMapping("/avec-candidatures")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<PosteAvecCandidaturesDto>> getPostesAvecCandidatures(
            @RequestParam(required = false) String departementNom,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String typeContrat,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean avecCandidatsUniquement,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        return ResponseEntity.ok(posteRecrutementServiceExtrait.getPostesAvecCandidatures(
                departementNom, status, typeContrat, workType, search,
                avecCandidatsUniquement, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','RH','CANDIDAT')")
    public ResponseEntity<PosteRecrutement> getPosteRecrutementById(@PathVariable String id) {
        return ResponseEntity.ok(posteRecrutementService.getById(id));
    }

    @GetMapping("/departement/{departementNom}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','RH','CANDIDAT')")
    public ResponseEntity<List<PosteRecrutement>> getPostesByDepartement(@PathVariable String departementNom) {
        return ResponseEntity.ok(posteRecrutementService.getPostesByDepartement(departementNom));
    }

    @PostMapping
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<PosteRecrutement> createPosteRecrutement(@RequestBody PosteRecrutementDto posteDto) {
        PosteRecrutement created = posteRecrutementService.createPosteRecrutement(posteDto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<PosteRecrutement> updatePosteRecrutement(
            @PathVariable String id,
            @RequestBody PosteRecrutementDto posteDto) {
        PosteRecrutement updated = posteRecrutementService.updatePosteRecrutement(id, posteDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<Void> deletePosteRecrutement(@PathVariable String id) {
        posteRecrutementService.deletePosteRecrutement(id);
        return ResponseEntity.noContent().build();
    }
}