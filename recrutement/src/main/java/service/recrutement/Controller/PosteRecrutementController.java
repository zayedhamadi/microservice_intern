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
@RequestMapping("/api/postesRecrutement")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLOYEE','RH')")
public class PosteRecrutementController {

    private final PosteRecrutementService posteRecrutementService;

    @PostMapping
    public ResponseEntity<PosteRecrutement> createPosteRecrutement(@RequestBody PosteRecrutementDto posteDto) {
        PosteRecrutement created = posteRecrutementService.createPosteRecrutement(posteDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosteRecrutement> getPosteRecrutementById(@PathVariable String id) {
        PosteRecrutement poste = posteRecrutementService.getById(id);
        return ResponseEntity.ok(poste);
    }

    @GetMapping("/departement/{departementNom}")
    public ResponseEntity<List<PosteRecrutement>> getPostesByDepartement(@PathVariable String departementNom) {
        List<PosteRecrutement> postes = posteRecrutementService.getPostesByDepartement(departementNom);
        return ResponseEntity.ok(postes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosteRecrutement(@PathVariable String id) {
        posteRecrutementService.deletePosteRecrutement(id);
        return ResponseEntity.noContent().build();
    }
}