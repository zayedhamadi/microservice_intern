package service.recrutement.Client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RankingClient {

    private final RestTemplate restTemplate;

    @Value("${ranking-service.url}")
    private String rankingServiceUrl;

    @Value("${internal.api.key}")
    private String internalApiKey;

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> scoreBatch(Object batchScoreRequest) {
        HttpEntity<Object> entity = new HttpEntity<>(batchScoreRequest, headers());
        return restTemplate.postForObject(rankingServiceUrl + "/score/batch", entity, List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> scoreBatchPostes(Object batchPostesRequest) {
        HttpEntity<Object> entity = new HttpEntity<>(batchPostesRequest, headers());
        return restTemplate.postForObject(rankingServiceUrl + "/score/batch-postes", entity, List.class);
    }
}