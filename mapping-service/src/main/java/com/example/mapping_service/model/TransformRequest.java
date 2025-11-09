package com.example.mapping_service.model;

import java.util.List;
import java.util.Map;

public class TransformRequest {
    private Map<String, Object> sourceData;
    private List<FieldMapping> mappings;

    public TransformRequest() {}

    public TransformRequest(Map<String, Object> sourceData, List<FieldMapping> mappings) {
        this.sourceData = sourceData;
        this.mappings = mappings;
    }

    public Map<String, Object> getSourceData() {
        return sourceData;
    }

    public void setSourceData(Map<String, Object> sourceData) {
        this.sourceData = sourceData;
    }

    public List<FieldMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<FieldMapping> mappings) {
        this.mappings = mappings;
    }
}
