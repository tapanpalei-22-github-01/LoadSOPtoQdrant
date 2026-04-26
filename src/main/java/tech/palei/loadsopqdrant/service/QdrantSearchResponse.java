package tech.palei.loadsopqdrant.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class QdrantSearchResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("time")
    private Double time;

    @JsonProperty("result")
    private List<QdrantPoint> result;

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTime() {
        return time;
    }

    public void setTime(Double time) {
        this.time = time;
    }

    public List<QdrantPoint> getResult() {
        return result;
    }

    public void setResult(List<QdrantPoint> result) {
        this.result = result;
    }
}