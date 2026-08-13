package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Enum.ApplicationStatus;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.PosteAvecCandidaturesDto;
import service.recrutement.Repository.ApplicationRepository;
import service.recrutement.Repository.PosteRecrutementRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PosteRecrutementServiceExtrait {

    private final ApplicationRepository applicationRepository;
    private final PosteRecrutementRepository posteRecrutementRepository;


    public List<PosteAvecCandidaturesDto> getPostesAvecCandidatures(
            String departementNom,
            String status,
            String typeContrat,
            String workType,
            String search,
            boolean avecCandidatsUniquement,
            String sortBy,
            String sortDir
    ) {
        List<PosteRecrutement> postes = posteRecrutementRepository.findAll();

        List<PosteAvecCandidaturesDto> enrichis = postes.stream()
                .filter(p -> departementNom == null || departementNom.isBlank()
                        || departementNom.equalsIgnoreCase(p.getDepartementNom()))
                .filter(p -> status == null || status.isBlank()
                        || status.equalsIgnoreCase(String.valueOf(p.getStatus())))
                .filter(p -> typeContrat == null || typeContrat.isBlank()
                        || typeContrat.equalsIgnoreCase(String.valueOf(p.getTypeContrat())))
                .filter(p -> workType == null || workType.isBlank()
                        || workType.equalsIgnoreCase(String.valueOf(p.getWorkType())))
                .filter(p -> search == null || search.isBlank()
                        || p.getTitre().toLowerCase().contains(search.toLowerCase()))
                .map(this::toDtoAvecStatistiques)
                .filter(dto -> !avecCandidatsUniquement || dto.getNombreCandidatures() > 0)
                .collect(Collectors.toList());

        Comparator<PosteAvecCandidaturesDto> comparator = getComparator(sortBy, sortDir);
        enrichis.sort(comparator);

        return enrichis;
    }

    @NonNull
    private static Comparator<PosteAvecCandidaturesDto> getComparator(String sortBy, String sortDir) {
        Comparator<PosteAvecCandidaturesDto> comparator = switch (sortBy == null ? "date" : sortBy) {
            case "candidatures" -> Comparator.comparingLong(PosteAvecCandidaturesDto::getNombreCandidatures);
            case "titre" -> Comparator.comparing(PosteAvecCandidaturesDto::getTitre, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    dto -> dto.getDatePosteRecrutement() == null ? "" : dto.getDatePosteRecrutement()
            );
        };
        if ("desc".equalsIgnoreCase(sortDir == null ? "desc" : sortDir)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private PosteAvecCandidaturesDto toDtoAvecStatistiques(PosteRecrutement poste) {
        List<Application> candidatures =
                applicationRepository.findByPosteRecrutementId(poste.getIdPosteRecrutement());

        long enAttente = candidatures.stream()
                .filter(c -> c.getStatut() == ApplicationStatus.EN_ATTENTE).count();
        long enEntretienRH = candidatures.stream()
                .filter(c -> c.getStatut() == ApplicationStatus.EN_ENTRETIEN_RH).count();
        long enEntretienTech = candidatures.stream()
                .filter(c -> c.getStatut() == ApplicationStatus.EN_ENTRETIEN_TECHNIQUE).count();
        long acceptees = candidatures.stream()
                .filter(c -> c.getStatut() == ApplicationStatus.ACCEPTE).count();
        long refusees = candidatures.stream()
                .filter(c -> c.getStatut() == ApplicationStatus.REJETE).count();

        return PosteAvecCandidaturesDto.builder()
                .idPosteRecrutement(poste.getIdPosteRecrutement())
                .titre(poste.getTitre())
                .departementNom(poste.getDepartementNom())
                .typeContrat(poste.getTypeContrat())
                .workType(poste.getWorkType())
                .status(poste.getStatus())
                .lieu(poste.getLieu())
                .salaire(poste.getSalaire() != null ? poste.getSalaire().doubleValue() : null)
                .nombrePostes(poste.getNombrePostes())
                .datePosteRecrutement(poste.getDatePosteRecrutement() != null
                        ? poste.getDatePosteRecrutement().toString() : null)
                .dateExpirationPosteRecrutement(poste.getDateExpirationPosteRecrutement() != null
                        ? poste.getDateExpirationPosteRecrutement().toString() : null)
                .competencesRequises(poste.getCompetencesRequises())
                .nombreCandidatures(candidatures.size())
                .nombreEnAttente(enAttente)
                .nombreEnEntretienRH(enEntretienRH)
                .nombreEnEntretienTechnique(enEntretienTech)
                .nombreAcceptees(acceptees)
                .nombreRefusees(refusees)
                .build();
    }
}