package service.recrutement.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.PosteRecrutementDto;
import service.recrutement.Service.PosteRecrutementService;

import java.util.List;

@RestController
@RequestMapping("/rh/api/postesRecrutement")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLOYEE','RH')")
public class PosteRecrutementController {

    private final PosteRecrutementService posteRecrutementService;

    @GetMapping
    public ResponseEntity<List<PosteRecrutement>> getAllPostes() {
        return ResponseEntity.ok(posteRecrutementService.getAllPostes());
    }

    @PostMapping
    public ResponseEntity<PosteRecrutement> createPosteRecrutement(@RequestBody PosteRecrutementDto posteDto) {
        PosteRecrutement created = posteRecrutementService.createPosteRecrutement(posteDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosteRecrutement> getPosteRecrutementById(@PathVariable String id) {
        return ResponseEntity.ok(posteRecrutementService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PosteRecrutement> updatePosteRecrutement(
            @PathVariable String id,
            @RequestBody PosteRecrutementDto posteDto) {
        PosteRecrutement updated = posteRecrutementService.updatePosteRecrutement(id, posteDto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/departement/{departementNom}")
    public ResponseEntity<List<PosteRecrutement>> getPostesByDepartement(@PathVariable String departementNom) {
        return ResponseEntity.ok(posteRecrutementService.getPostesByDepartement(departementNom));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosteRecrutement(@PathVariable String id) {
        posteRecrutementService.deletePosteRecrutement(id);
        return ResponseEntity.noContent().build();
    }
}