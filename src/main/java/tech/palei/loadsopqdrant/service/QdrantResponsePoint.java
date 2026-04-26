package tech.palei.loadsopqdrant.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QdrantResponsePoint {

    @JsonProperty("result")
    private QdrantPoint point;
    private String status;
    private Double time;

    public QdrantPoint getPoint() {
        return point;
    }

    public void setPoint(QdrantPoint point) {
        this.point = point;
    }

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
}