package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import service.recrutement.Client.UserServiceClient;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.CandidatDto;
import service.recrutement.Entity.dto.DepartementDto;
import service.recrutement.Entity.dto.PosteRecrutementDto;
import service.recrutement.Mail.RecrutementMail;
import service.recrutement.Repository.PosteRecrutementRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PosteRecrutementService {

    private final PosteRecrutementRepository posteRecrutementRepository;
    private final UserServiceClient userServiceClient;
    private final RecrutementMail recrutementMail;

    public PosteRecrutement getById(String id) {
        return this.posteRecrutementRepository.findByIdPosteRecrutement(id)
                .orElseThrow(() -> new RuntimeException("JobOffer not found with id: " + id));
    }

    public PosteRecrutement deletePosteRecrutement(String id) {
        PosteRecrutement posteRecrutement = this.posteRecrutementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("JobOffer not found with id: " + id));

        log.info("Delete poste recrutement with id: {} - {}", id, posteRecrutement.getTitre());
        this.posteRecrutementRepository.deleteById(id);
        return posteRecrutement;
    }

    public PosteRecrutement createPosteRecrutement(PosteRecrutementDto posteDto) {
        if (posteDto.getDepartementNom() == null || posteDto.getDepartementNom().isBlank()) {
            throw new RuntimeException("Le département du poste est obligatoire");
        }

        DepartementDto departement = userServiceClient.getDepartementByNom(posteDto.getDepartementNom());
        if (departement == null) {
            throw new RuntimeException("Département introuvable : " + posteDto.getDepartementNom());
        }

        PosteRecrutement poste = PosteRecrutement.builder()
                .recruteurKeycloakId(posteDto.getRecruteurKeycloakId())
                .titre(posteDto.getTitre())
                .description(posteDto.getDescription())
                .profilDemandeOfPoste(posteDto.getProfilDemandeOfPoste())
                .competencesRequises(posteDto.getCompetencesRequises())
                .languesRequises(posteDto.getLanguesRequises())
                .anneesExperienceMin(posteDto.getAnneesExperienceMin())
                .niveauEtudeRequis(posteDto.getNiveauEtudeRequis())
                .typeContrat(posteDto.getTypeContrat())
                .status(posteDto.getStatus() != null ? posteDto.getStatus() : StatusPosteRecrutement.OUVERT)
                .workType(posteDto.getWorkType())
                .lieu(posteDto.getLieu())
                .salaire(posteDto.getSalaire())
                .nombrePostes(posteDto.getNombrePostes() != null ? posteDto.getNombrePostes() : 1)
                .departementNom(posteDto.getDepartementNom())
                .datePosteRecrutement(posteDto.getDatePosteRecrutement())
                .dateExpirationPosteRecrutement(posteDto.getDateExpirationPosteRecrutement())
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();

        PosteRecrutement savedPoste = posteRecrutementRepository.save(poste);
        log.info("Poste de recrutement créé avec succès : {} (département {})",
                savedPoste.getTitre(), savedPoste.getDepartementNom());

        notifyCandidatesOfNewPoste(savedPoste);

        return savedPoste;
    }

    public List<PosteRecrutement> getPostesByDepartement(String departementNom) {
        return posteRecrutementRepository.findByDepartementNom(departementNom);
    }

    private void notifyCandidatesOfNewPoste(PosteRecrutement poste) {
        List<CandidatDto> candidats = userServiceClient.getAllCandidats();

        if (candidats.isEmpty()) {
            log.warn("Aucun candidat trouvé, aucune notification envoyée pour le poste {}", poste.getTitre());
            return;
        }

        for (CandidatDto candidat : candidats) {
            recrutementMail.sendNewPosteNotification(candidat.getEmail(), candidat.getPrenom(), poste);
        }

        log.info("Notification envoyée à {} candidat(s) pour le nouveau poste : {}",
                candidats.size(), poste.getTitre());
    }
}