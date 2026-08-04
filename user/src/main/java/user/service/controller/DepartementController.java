package user.service.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import user.service.Dto.DepartementDTO;
import user.service.Entity.Departement;
import user.service.Serivce.DepartementService;

import java.util.List;
@PreAuthorize("hasAnyRole('EMPLOYEE','RH')")
@RestController
@RequestMapping("/api/departements")
@RequiredArgsConstructor
public class DepartementController {

    private final DepartementService departementService;

    @PostMapping
    public ResponseEntity<Departement> createDepartement(@RequestBody DepartementDTO departementDTO) {
        Departement created = departementService.createDepartement(departementDTO);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<DepartementDTO>> getAllDepartements() {
        List<DepartementDTO> departements = departementService.getAllDepartements();
        return ResponseEntity.ok(departements);
    }

    @GetMapping("/{nom}")
    public ResponseEntity<DepartementDTO> getDepartementByName(@PathVariable String nom) {
        DepartementDTO departement = departementService.searchDepartementByName(nom);
        if (departement == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(departement);
    }

    @PutMapping("/{nom}")
    public ResponseEntity<Departement> updateDepartement(
            @PathVariable String nom,
            @RequestBody DepartementDTO departementDTO) {
        Departement updated = departementService.modifyDepartement(nom, departementDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{nom}")
    public ResponseEntity<Void> deleteDepartement(@PathVariable String nom) {
        departementService.deleteDepartement(nom);
        return ResponseEntity.noContent().build();
    }
}