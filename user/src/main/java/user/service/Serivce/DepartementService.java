package user.service.Serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import user.service.Dto.DepartementDTO;
import user.service.Entity.Departement;
import user.service.Repository.DepartementRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartementService {

    private final DepartementRepository departementRepository;

    public Departement findByDepartementName(String nom) {
        return departementRepository.findByNom(nom.trim())
                .orElseThrow(() -> new RuntimeException("Département introuvable : " + nom));
    }

    public Departement createDepartement(DepartementDTO departementDTO) {
        String nom = departementDTO.getNom().trim();

        if (departementRepository.findByNom(nom).isPresent()) {
            throw new RuntimeException("Le département existe déjà : " + nom);
        }

        Departement departement = Departement.builder()
                .nom(nom)
                .description(departementDTO.getDescription())
                .dateCreation(LocalDate.now())
                .build();

        Departement savedDepartement = departementRepository.save(departement);
        log.info("Département créé avec succès : {}", savedDepartement.getNom());
        return savedDepartement;
    }

    public void deleteDepartement(String nom) {
        Departement departement = findByDepartementName(nom);
        departementRepository.delete(departement);
        log.info("Département supprimé avec succès : {}", nom);
    }

    public Departement modifyDepartement(String nom, DepartementDTO departementDTO) {
        Departement existingDepartement = findByDepartementName(nom);
        String newNom = departementDTO.getNom().trim();

        if (!existingDepartement.getNom().equals(newNom) &&
                departementRepository.findByNom(newNom).isPresent()) {
            throw new RuntimeException("Le département " + newNom + " existe déjà");
        }

        existingDepartement.setNom(newNom);
        existingDepartement.setDescription(departementDTO.getDescription());

        Departement updatedDepartement = departementRepository.save(existingDepartement);
        log.info("Département modifié avec succès : {}", updatedDepartement.getNom());
        return updatedDepartement;
    }

    public DepartementDTO searchDepartementByName(String nom) {
        return departementRepository.findByNom(nom.trim())
                .map(d -> new DepartementDTO(d.getNom(), d.getDescription()))
                .orElse(null);
    }

    public List<DepartementDTO> getAllDepartements() {
        return departementRepository.findAll().stream()
                .map(d -> new DepartementDTO(d.getNom(), d.getDescription()))
                .collect(Collectors.toList());
    }
}