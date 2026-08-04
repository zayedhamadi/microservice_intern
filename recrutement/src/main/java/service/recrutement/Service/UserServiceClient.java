package service.recrutement.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    public UserServiceClient(RestTemplate restTemplate,
                             @Value("${user-service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }


    public boolean isTokenBlacklisted(String jti) {
        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(
                    userServiceUrl + "/api/auth/blacklist/{jti}",
                    Boolean.class,
                    jti
            );
            Boolean body = response.getBody();
            return body != null && body;
        } catch (HttpClientErrorException.NotFound e) {
            // jti non trouvé dans la blacklist => token valide
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification blacklist (jti {})", jti, e);
            throw e;
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

    /**
     * Récupère la liste des utilisateurs ayant le rôle CANDIDAT, pour notification email.
     * ADAPTE le chemin ci-dessous si l'endpoint réel de user-service diffère
     * (ex : /api/utilisateurs/role/CANDIDAT, /api/candidats, etc.).
     */
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