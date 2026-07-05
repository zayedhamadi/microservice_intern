package user.service.Serivce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import user.service.Dto.AuthResponse;
import user.service.Dto.LoginRequest;
import user.service.Dto.RegisterRequest;
import user.service.Dto.UpdateUSerAfterConnect;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    private final RestTemplate restTemplate;

    private String getAdminToken() {
        String url = serverUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", adminUsername);
        body.add("password", adminPassword);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new RuntimeException("Impossible d'obtenir le token admin Keycloak");
            }
            return (String) responseBody.get("access_token");
        } catch (HttpClientErrorException e) {
            log.error("Erreur auth admin Keycloak : {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erreur authentification admin Keycloak : " + e.getMessage());
        }
    }

    public String createUserInKeycloak(RegisterRequest dto) {
        String adminToken = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", dto.getPassword());
        credential.put("temporary", false);

        Map<String, Object> userBody = new HashMap<>();
        userBody.put("username", dto.getEmail());
        userBody.put("email", dto.getEmail());
        userBody.put("firstName", dto.getPrenom());
        userBody.put("lastName", dto.getNom());
        userBody.put("enabled", true);
        userBody.put("emailVerified", true);
        userBody.put("credentials", List.of(credential));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
            String location = Objects.requireNonNull(response.getHeaders().getLocation()).toString();
            String keycloakId = location.substring(location.lastIndexOf("/") + 1);
            log.info("User créé dans Keycloak : {}", keycloakId);

            assignRoleToUser(keycloakId, dto.getRole().name(), adminToken);
            return keycloakId;

        } catch (HttpClientErrorException e) {
            log.error("Erreur création user Keycloak : {}", e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new RuntimeException("Email déjà utilisé dans Keycloak");
            }
            throw new RuntimeException("Erreur Keycloak : " + e.getMessage());
        }
    }

    private void assignRoleToUser(String keycloakId, String roleName, String adminToken) {
        try {
            String roleUrl = serverUrl + "/admin/realms/" + realm + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            ResponseEntity<Map<String, Object>> roleResponse = restTemplate.exchange(
                    roleUrl, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> roleInfo = Objects.requireNonNull(roleResponse.getBody());

            String assignUrl = serverUrl + "/admin/realms/" + realm
                    + "/users/" + keycloakId + "/role-mappings/realm";

            HttpHeaders assignHeaders = new HttpHeaders();
            assignHeaders.setContentType(MediaType.APPLICATION_JSON);
            assignHeaders.setBearerAuth(adminToken);

            restTemplate.postForEntity(assignUrl, new HttpEntity<>(List.of(roleInfo), assignHeaders), Void.class);
            log.info("Role '{}' assigné au user '{}'", roleName, keycloakId);

        } catch (HttpClientErrorException e) {
            log.error("Erreur assignation role : {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erreur assignation role : " + e.getMessage());
        }
    }

    public void assignRoleInKeycloak(String keycloakId, String roleName) {
        assignRoleToUser(keycloakId, roleName, getAdminToken());
    }

    public AuthResponse login(LoginRequest dto) {
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", dto.getEmail());
        body.add("password", dto.getPassword());
        body.add("scope", "openid");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> tokenData = Objects.requireNonNull(response.getBody());

            return AuthResponse.builder()
                    .accessToken((String) tokenData.get("access_token"))
                    .refreshToken((String) tokenData.get("refresh_token"))
                    .tokenType("Bearer")
                    .expiresIn(Long.valueOf(tokenData.get("expires_in").toString()))
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Erreur login Keycloak : {}", e.getResponseBodyAsString());
            throw new RuntimeException("Email ou mot de passe incorrect");
        }
    }

    public void deleteUserFromKeycloak(String keycloakId) {
        String adminToken = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        log.info("User '{}' supprimé de Keycloak", keycloakId);
    }

    public void changePassword(String keycloakId, String newPassword) {
        String adminToken = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> body = new HashMap<>();
        body.put("type", "password");
        body.put("value", newPassword);
        body.put("temporary", false);

        restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);
        log.info("Password changé pour {}", keycloakId);
    }

    public void updateUserInKeycloak(String keycloakId, UpdateUSerAfterConnect dto) {
        String adminToken = getAdminToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> body = new HashMap<>();
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            body.put("email", dto.getEmail());
            body.put("emailVerified", true);
        }
        if (dto.getNom() != null) body.put("lastName", dto.getNom());
        if (dto.getPrenom() != null) body.put("firstName", dto.getPrenom());

        if (body.isEmpty()) return;

        try {
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);
            log.info("User Keycloak '{}' mis à jour : {}", keycloakId, body.keySet());
        } catch (HttpClientErrorException e) {
            log.error("Erreur mise à jour Keycloak : {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erreur mise à jour Keycloak : " + e.getMessage());
        }
    }
}