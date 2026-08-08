package service.recrutement.Client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import service.recrutement.Entity.dto.CandidatDto;
import service.recrutement.Entity.dto.DepartementDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String internalApiKey;

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    public UserServiceClient(RestTemplate restTemplate,
                             @Value("${user-service.url}") String userServiceUrl,
                             @Value("${internal.api.key}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.internalApiKey = internalApiKey;
    }

    public CandidatDto getCandidatByKeycloakId(String keycloakId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(INTERNAL_API_KEY_HEADER, internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<CandidatDto> response = restTemplate.exchange(
                    userServiceUrl + "/internal/users/{keycloakId}/candidat-info",
                    HttpMethod.GET,
                    entity,
                    CandidatDto.class,
                    keycloakId
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Candidat introuvable : " + keycloakId);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à user-service (candidat {})", keycloakId, e);
            throw new RuntimeException("Impossible de récupérer les infos du candidat : " + keycloakId, e);
        }
    }

    public DepartementDto getDepartementByNom(String nom) {
        try {
            ResponseEntity<DepartementDto> response = restTemplate.getForEntity(
                    userServiceUrl + "/api/departements/{nom}",
                    DepartementDto.class,
                    nom
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Département introuvable : " + nom);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à user-service (département {})", nom, e);
            throw new RuntimeException("Impossible de vérifier le département : " + nom, e);
        }
    }

    public List<CandidatDto> getAllCandidats() {
        try {
            ResponseEntity<CandidatDto[]> response = restTemplate.exchange(
                    userServiceUrl + "/api/utilisateurs/role/CANDIDAT",
                    HttpMethod.GET,
                    null,
                    CandidatDto[].class
            );
            CandidatDto[] body = response.getBody();
            return body == null ? Collections.emptyList() : List.of(body);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des candidats depuis user-service", e);
            return Collections.emptyList();
        }
    }
}