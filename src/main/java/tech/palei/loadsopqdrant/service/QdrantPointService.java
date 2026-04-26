package tech.palei.loadsopqdrant.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class QdrantPointService {

    private final WebClient webClient;

    public QdrantPointService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:6333").build();
    }

    // CREATE
    public void insert(QdrantPoint point) {
        Map<String, Object> pointData = new HashMap<>();
        pointData.put("id", point.getId().toString());
        pointData.put("vector", point.getVector());
        pointData.put("payload", point.getPayload());

        Map<String, Object> request = new HashMap<>();
        request.put("points", List.of(pointData));

        webClient.post()
                .uri("/collections/my_collection/points")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    // READ
    public QdrantResponsePoint get(UUID id) {
        return webClient.get()
                .uri("/collections/my_collection/points/" + id)
                .retrieve()
                .bodyToMono(QdrantResponsePoint.class)
                .block();
    }

    // UPDATE (same as insert, Qdrant upsert overwrites)
    public void update(QdrantPoint point) {
        insert(point);
    }

    // DELETE
    public void delete(UUID id) {
        Map<String, Object> request = new HashMap<>();
        request.put("points", List.of(id.toString()));

        webClient.post()
                .uri("/collections/my_collection/points/delete")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    // SEARCH
    public void search(float[] queryVector) {
        List<Float> vectorList = new ArrayList<>();
        for (float f : queryVector) {
            vectorList.add(f);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("vector", vectorList);
        request.put("limit", 5);

        webClient.post()
                .uri("/collections/my_collection/points/search")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
