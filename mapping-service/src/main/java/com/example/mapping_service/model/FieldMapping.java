package com.example.mapping_service.model;

public class FieldMapping {
    private String sourcePath;
    private String targetPath;
    private String transformationType;
    private double confidence;

    public FieldMapping() {}

    public FieldMapping(String sourcePath, String targetPath, String transformationType, double confidence) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.transformationType = transformationType;
        this.confidence = confidence;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTransformationType() {
        return transformationType;
    }

    public void setTransformationType(String transformationType) {
        this.transformationType = transformationType;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "FieldMapping{" +
                "sourcePath='" + sourcePath + '\'' +
                ", targetPath='" + targetPath + '\'' +
                ", transformationType='" + transformationType + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
