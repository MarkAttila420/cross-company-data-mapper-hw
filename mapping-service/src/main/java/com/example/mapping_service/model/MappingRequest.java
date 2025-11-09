package com.example.mapping_service.model;

import java.util.Map;

public class MappingRequest {
    private Map<String, Object> sourceFormat;
    private Map<String, Object> targetFormat;

    public MappingRequest() {}

    public MappingRequest(Map<String, Object> sourceFormat, Map<String, Object> targetFormat) {
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
    }

    public Map<String, Object> getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(Map<String, Object> sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public Map<String, Object> getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(Map<String, Object> targetFormat) {
        this.targetFormat = targetFormat;
    }
}
