package com.example.mapping_service.model;

import java.util.List;

public class MappingResponse {
    private List<FieldMapping> mappings;

    public MappingResponse() {}

    public MappingResponse(List<FieldMapping> mappings) {
        this.mappings = mappings;
    }

    public List<FieldMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<FieldMapping> mappings) {
        this.mappings = mappings;
    }
}
