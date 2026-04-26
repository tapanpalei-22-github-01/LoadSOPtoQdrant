package tech.palei.loadsopqdrant.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.ArrayList;

public class QdrantCrudService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public QdrantCrudService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // CREATE
    public void insert(QdrantPoint point) throws Exception {
        Map<String, Object> pointData = new HashMap<>();
        pointData.put("id", point.getId().toString());
        pointData.put("vector", point.getVector());
        pointData.put("payload", point.getPayload());

        Map<String, Object> request = new HashMap<>();
        request.put("points", List.of(pointData));

        String json = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/my_collection/points"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Insert failed: " + response.body());
        }
    }

    // READ
    public QdrantResponsePoint get(UUID id) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/my_collection/points/" + id))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }

        QdrantResponsePoint responsePoint = objectMapper.readValue(response.body(), QdrantResponsePoint.class);
        return responsePoint;
    }

    // UPDATE (same as insert, Qdrant upsert overwrites)
    public void update(QdrantPoint point) throws Exception {
        insert(point);
    }

    // DELETE
    public void delete(UUID id) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("points", List.of(id.toString()));

        String json = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/my_collection/points/delete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Delete failed: " + response.body());
        }
    }

    // SEARCH
    public List<QdrantResponsePoint> search(float[] queryVector) throws Exception {
        List<Float> vectorList = new ArrayList<>();
        for (float f : queryVector) {
            vectorList.add(f);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("vector", vectorList);
        request.put("limit", 5);

        String json = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/my_collection/points/search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Search failed: " + response.body());
        }

        QdrantSearchResponse searchResponse = objectMapper.readValue(response.body(), QdrantSearchResponse.class);

        List<QdrantResponsePoint> responsePoints = new ArrayList<>();
        for (QdrantPoint point : searchResponse.getResult()) {
            QdrantResponsePoint rp = new QdrantResponsePoint();
            rp.setPoint(point);
            rp.setStatus(searchResponse.getStatus());
            rp.setTime(searchResponse.getTime());
            responsePoints.add(rp);
        }

        return responsePoints;
    }
}
