package com.example.mapping_service.model;

import java.util.Map;

public class TransformResponse {
    private Map<String, Object> transformedData;

    public TransformResponse() {}

    public TransformResponse(Map<String, Object> transformedData) {
        this.transformedData = transformedData;
    }

    public Map<String, Object> getTransformedData() {
        return transformedData;
    }

    public void setTransformedData(Map<String, Object> transformedData) {
        this.transformedData = transformedData;
    }
}
