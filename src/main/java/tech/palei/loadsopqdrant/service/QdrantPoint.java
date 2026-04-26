package tech.palei.loadsopqdrant.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QdrantPoint {
    private UUID id;
    private List<Float> vector;
    private Map<String, Object> payload;

    public QdrantPoint(UUID id, List<Float> vector, Map<String, Object> payload) {
        this.id = id;
        this.vector = vector;
        this.payload = payload;
    }

    // getters and setters
}
