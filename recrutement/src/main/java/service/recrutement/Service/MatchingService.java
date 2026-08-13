package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import service.recrutement.Client.RankingClient;
import service.recrutement.Client.UserServiceClient;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final PosteRecrutementService posteRecrutementService;
    private final UserServiceClient userServiceClient;
    private final RankingClient rankingClient;

    public List<PosteClasseDto> getPostesClassesPourCandidat(String candidatKeycloakId) {
        List<PosteRecrutement> postesOuverts = posteRecrutementService.getAllPostes().stream()
                .filter(PosteRecrutement::isOuvert)
                .toList();

        if (postesOuverts.isEmpty()) return List.of();

        CandidatDto candidat = userServiceClient.getCandidatByKeycloakId(candidatKeycloakId);
        if (candidat == null) {
            throw new RuntimeException("Profil candidat introuvable, complète ton profil avant de voir tes recommandations");
        }

        MlCandidatDto candidatMl = MlCandidatDto.builder()
                .keycloakId(candidatKeycloakId)
                .competences(candidat.getCompetences())
                .langues(candidat.getLangues())
                .anneesExperience(candidat.getAnneesExperience())
                .niveauEtude(candidat.getNiveauEtude())
                .typeContratSouhaite(candidat.getTypeContratSouhaite())
                .lieu(candidat.getLieu())
                .salaireAttendu(candidat.getSalaireAttendu())
                .certifications(candidat.getCertifications())
                .build();

        List<MlPosteDto> postesMl = postesOuverts.stream().map(p -> MlPosteDto.builder()
                .idPosteRecrutement(p.getIdPosteRecrutement())
                .competencesRequises(p.getCompetencesRequises())
                .languesRequises(p.getLanguesRequises())
                .anneesExperienceMin(p.getAnneesExperienceMin())
                .niveauEtudeRequis(p.getNiveauEtudeRequis())
                .typeContrat(p.getTypeContrat() != null ? p.getTypeContrat().name() : null)
                .workType(p.getWorkType() != null ? p.getWorkType().name() : null)
                .lieu(p.getLieu())
                .salaire(p.getSalaire() != null ? p.getSalaire().doubleValue() : null)
                .build()
        ).toList();

        Map<String, Object> body = Map.of("candidat", candidatMl, "postes", postesMl);

        List<Map<String, Object>> scores;
        try {
            scores = rankingClient.scoreBatchPostes(body);
        } catch (Exception e) {
            log.warn("Ranking-service injoignable pour le candidat {} : {}", candidatKeycloakId, e.getMessage());
            return List.of();
        }

        Map<String, PosteRecrutement> parId = postesOuverts.stream()
                .collect(Collectors.toMap(PosteRecrutement::getIdPosteRecrutement, p -> p));

        return scores.stream()
                .map(s -> {
                    String id = (String) s.get("idPosteRecrutement");
                    PosteRecrutement p = parId.get(id);
                    return PosteClasseDto.builder()
                            .idPosteRecrutement(id)
                            .titre(p.getTitre())
                            .departementNom(p.getDepartementNom())
                            .score(((Number) s.get("score")).doubleValue())
                            .build();
                })
                .toList();
    }
}